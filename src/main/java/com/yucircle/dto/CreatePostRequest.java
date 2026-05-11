package com.yucircle.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePostRequest {
    private String title;
    private String content;
    private List<String> images;
    private String category;
}
