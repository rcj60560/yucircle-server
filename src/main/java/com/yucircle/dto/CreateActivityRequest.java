package com.yucircle.dto;

import lombok.Data;

@Data
public class CreateActivityRequest {
    private Long venueId;
    private Long clubId;
    private String title;
    private String activityDate;
    private String timeSlot;
    private String matchType;
    private Integer courtCount;
    private Integer maxPerCourt;
}
