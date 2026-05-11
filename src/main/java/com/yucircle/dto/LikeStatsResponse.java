package com.yucircle.dto;

import lombok.Data;

@Data
public class LikeStatsResponse {
    private String objectType;
    private Long objectId;
    private long likeCount;
    private long dislikeCount;
    private String myAction;

    public LikeStatsResponse(String objectType, Long objectId, long likeCount, long dislikeCount, String myAction) {
        this.objectType = objectType;
        this.objectId = objectId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.myAction = myAction;
    }
}
