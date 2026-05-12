package com.yucircle.dto;

import lombok.Data;

@Data
public class BroadcastDto {
    private Long id;
    private String content;
    private String expiresAt;
}
