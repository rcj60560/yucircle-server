package com.yucircle.dto;

import lombok.Data;

@Data
public class LikeRequest {
    private String objectType;
    private Long objectId;
    private String type;
}
