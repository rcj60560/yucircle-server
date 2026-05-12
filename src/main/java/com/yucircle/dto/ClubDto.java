package com.yucircle.dto;

import lombok.Data;

@Data
public class ClubDto {
    private Long id;
    private String name;
    private String description;
    private Integer todayActivityCount;
    private Integer onlineCount;
    private Boolean hasBroadcast;
    private String status;
}
