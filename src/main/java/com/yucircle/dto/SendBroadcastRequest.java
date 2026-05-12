package com.yucircle.dto;

import lombok.Data;

@Data
public class SendBroadcastRequest {
    private String content;
    private Integer expiresInSeconds;
}
