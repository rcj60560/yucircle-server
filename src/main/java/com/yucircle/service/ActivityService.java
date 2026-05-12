package com.yucircle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yucircle.dto.*;
import com.yucircle.entity.Activity;

import java.time.LocalDateTime;

public interface ActivityService extends IService<Activity> {

    Activity createActivity(Long userId, CreateActivityRequest request);

    Activity enterLobby(Long userId, Long activityId);

    ActivityLobbyDto getLobbySnapshot(Long activityId);

    ActivitySlotDto reserveSlot(Long userId, Long activityId, ReserveSlotRequest request);

    void cancelSlot(Long userId, Long activityId);

    ActivitySlotDto confirmSlot(Long userId, Long activityId);

    ActivityMessageDto sendMessage(Long userId, Long activityId, SendActivityMessageRequest request);

    void ping(Long userId, Long activityId);

    void leaveLobby(Long userId, Long activityId);

    void cleanupExpiredPresence();

    void cleanupExpiredSlots();

    void cleanupOldMessages();
}
