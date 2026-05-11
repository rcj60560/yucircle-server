package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yucircle.dto.LoginResponse;
import com.yucircle.dto.SendSmsCodeRequest;
import com.yucircle.dto.SetupProfileRequest;
import com.yucircle.dto.VerifySmsCodeRequest;
import com.yucircle.entity.SmsCode;
import com.yucircle.entity.User;
import com.yucircle.mapper.SmsCodeMapper;
import com.yucircle.mapper.UserMapper;
import com.yucircle.util.JwtUtils;
import com.yucircle.util.SmsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final SmsCodeMapper smsCodeMapper;
    private final JwtUtils jwtUtils;
    private final SmsUtils smsUtils;

    public void sendCode(SendSmsCodeRequest request) {
        String phone = request.getPhone();
        String code = smsUtils.sendCode(phone);

        // 使旧验证码失效
        LambdaQueryWrapper<SmsCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsCode::getPhone, phone).eq(SmsCode::getIsUsed, false);
        SmsCode old = smsCodeMapper.selectOne(wrapper);
        if (old != null) {
            old.setIsUsed(true);
            smsCodeMapper.updateById(old);
        }

        // 保存新验证码（5分钟有效）
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setAttempts(0);
        smsCode.setIsUsed(false);
        smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        smsCode.setCreatedAt(LocalDateTime.now());
        smsCodeMapper.insert(smsCode);
    }

    public LoginResponse verifyCode(VerifySmsCodeRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        LambdaQueryWrapper<SmsCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getIsUsed, false)
                .gt(SmsCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(SmsCode::getCreatedAt)
                .last("LIMIT 1");

        SmsCode smsCode = smsCodeMapper.selectOne(wrapper);
        if (smsCode == null) {
            throw new RuntimeException("验证码不存在或已过期");
        }
        if (!smsCode.getCode().equals(code)) {
            smsCode.setAttempts(smsCode.getAttempts() + 1);
            smsCodeMapper.updateById(smsCode);
            throw new RuntimeException("验证码错误");
        }

        // 标记已使用
        smsCode.setIsUsed(true);
        smsCodeMapper.updateById(smsCode);

        // 查找或创建用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(userWrapper);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        String token = jwtUtils.generateToken(user.getId());
        return new LoginResponse(token, user);
    }

    public User getMe(Long userId) {
        return userMapper.selectById(userId);
    }

    public User setupProfile(Long userId, SetupProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getBadmintonLevel() != null) user.setBadmintonLevel(request.getBadmintonLevel());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }
}
