package com.yucircle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.BroadcastDto;
import com.yucircle.dto.SendBroadcastRequest;
import com.yucircle.entity.Broadcast;
import com.yucircle.mapper.BroadcastMapper;
import com.yucircle.service.BroadcastService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BroadcastServiceImpl extends ServiceImpl<BroadcastMapper, Broadcast> implements BroadcastService {

    @Override
    public List<BroadcastDto> listActiveBroadcasts(Long clubId) {
        LambdaQueryWrapper<Broadcast> query = new LambdaQueryWrapper<>();
        query.eq(Broadcast::getClubId, clubId)
             .gt(Broadcast::getExpiresAt, LocalDateTime.now())
             .orderByDesc(Broadcast::getCreatedAt);
        List<Broadcast> broadcasts = list(query);

        return broadcasts.stream().map(b -> {
            BroadcastDto dto = new BroadcastDto();
            dto.setId(b.getId());
            dto.setContent(b.getContent());
            dto.setExpiresAt(b.getExpiresAt().toString());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Broadcast sendBroadcast(Long userId, Long clubId, SendBroadcastRequest request) {
        Broadcast broadcast = new Broadcast();
        broadcast.setClubId(clubId);
        broadcast.setSenderId(userId);
        broadcast.setContent(request.getContent());
        broadcast.setExpiresAt(LocalDateTime.now().plusSeconds(request.getExpiresInSeconds()));
        broadcast.setCreatedAt(LocalDateTime.now());
        save(broadcast);
        return broadcast;
    }

    @Override
    public void cleanupExpiredBroadcasts() {
        LambdaQueryWrapper<Broadcast> query = new LambdaQueryWrapper<>();
        query.le(Broadcast::getExpiresAt, LocalDateTime.now());
        remove(query);
    }
}
