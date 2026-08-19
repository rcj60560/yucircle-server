package com.yucircle.dto;

import lombok.Data;

@Data
public class ClubActivityDto {
    private Long id;
    private String title;
    private String activityDate;
    private String timeSlot;
    private String description;
    private String organizerName;
    private Integer onlineCount;
    private String status;
    private Integer courtCount;
    private Integer maxPerCourt;
}
