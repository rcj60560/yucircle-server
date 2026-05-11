package com.yucircle.dto;

import lombok.Data;

@Data
public class CreatePostRequest {
    private String title;
    private String content;
    private String images;  // 改为 String，接收逗号分隔的图片 URL
    private String category;
}
