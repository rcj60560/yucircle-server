package com.yucircle.dto;

import lombok.Data;

@Data
public class ReserveSlotRequest {
    private Integer courtNumber;
    private Integer slotNumber;
}
