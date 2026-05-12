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
}
