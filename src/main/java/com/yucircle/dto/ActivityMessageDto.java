package com.yucircle.dto;

import lombok.Data;

@Data
public class ActivityMessageDto {
    private Long id;
    private String nickname;
    private String level;
    private String content;
    private String createdAt;
}
