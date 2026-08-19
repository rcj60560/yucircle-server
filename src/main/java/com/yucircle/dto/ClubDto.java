package com.yucircle.dto;

import lombok.Data;

@Data
public class ClubDto {
    private Long id;
    private Long venueId;
    private String name;
    private String description;
    private Long creatorId;
    private Integer broadcastCredits;
    private String createdAt;
    private Integer todayActivityCount;
    private Integer onlineCount;
    private Boolean hasActiveBoradcast;
    private Long nextActivityId;
    private String nextActivityTitle;
    private String nextActivityTime;
    private String nextActivityDescription;
    private Integer nextActivityOnlineCount;
    private String organizerName;
    private String status;
}
