package com.yucircle.dto;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private Long postId;
    private String content;
    private Long parentId;
    private Long rootId;
}
