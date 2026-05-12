package com.yucircle.dto;

import lombok.Data;

@Data
public class ActivitySlotDto {
    private Long slotId;
    private Integer courtNumber;
    private Integer slotNumber;
    private Long userId;
    private String nickname;
    private String avatar;
    private String level;
    private String status;
}
