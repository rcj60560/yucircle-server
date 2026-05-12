package com.yucircle.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActivityLobbyDto {
    private Long activityId;
    private String title;
    private String venueName;
    private String clubName;
    private String timeSlot;
    private String status;
    private Integer onlineCount;
    private BroadcastDto broadcast;
    private List<CourtDto> courts;
    private List<ObserverDto> observers;
    private List<ActivityMessageDto> messages;
}
