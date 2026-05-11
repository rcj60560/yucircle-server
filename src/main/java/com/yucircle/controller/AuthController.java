package com.yucircle.controller;

import com.yucircle.dto.*;
import com.yucircle.entity.User;
import com.yucircle.service.AuthService;
import com.yucircle.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @PostMapping("/send-code")
    public ApiResponse<String> sendCode(@RequestBody SendSmsCodeRequest request) {
        authService.sendCode(request);
        return ApiResponse.success("验证码已发送（模拟：123456）");
    }

    @PostMapping("/verify-code")
    public ApiResponse<LoginResponse> verifyCode(@RequestBody VerifySmsCodeRequest request) {
        LoginResponse response = authService.verifyCode(request);
        return ApiResponse.success(response, "登录成功");
    }

    @GetMapping("/me")
    public ApiResponse<User> getMe(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        User user = authService.getMe(userId);
        return ApiResponse.success(user);
    }

    @PutMapping("/profile")
    public ApiResponse<User> setupProfile(@RequestBody SetupProfileRequest request,
                                          HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        User user = authService.setupProfile(userId, request);
        return ApiResponse.success(user, "资料更新成功");
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }
}
