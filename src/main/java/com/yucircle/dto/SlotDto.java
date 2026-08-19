package com.yucircle.dto;

import lombok.Data;

@Data
public class SlotDto {
    private Integer slotNumber;
    private Long userId;
    private String nickname;
    private String avatar;
    private String level;
    private String status;
    private Boolean isMine; // true if this slot is for current user
}
