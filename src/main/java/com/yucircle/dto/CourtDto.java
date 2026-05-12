package com.yucircle.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourtDto {
    private Integer courtNumber;
    private String status;
    private Integer confirmedCount;
    private List<SlotDto> slots;
}
