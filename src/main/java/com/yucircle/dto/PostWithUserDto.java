package com.yucircle.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子与用户信息合并 DTO
 */
@Data
public class PostWithUserDto {
    private Long id;
    private Long userId;
    private String nickname;      // 用户昵称
    private String avatar;        // 用户头像
    private String title;
    private String content;
    private String images;
    private String category;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Integer viewCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
