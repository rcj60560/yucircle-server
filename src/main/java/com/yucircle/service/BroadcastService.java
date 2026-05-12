package com.yucircle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yucircle.dto.BroadcastDto;
import com.yucircle.dto.SendBroadcastRequest;
import com.yucircle.entity.Broadcast;

import java.util.List;

public interface BroadcastService extends IService<Broadcast> {

    List<BroadcastDto> listActiveBroadcasts(Long clubId);

    Broadcast sendBroadcast(Long userId, Long clubId, SendBroadcastRequest request);

    void cleanupExpiredBroadcasts();
}
