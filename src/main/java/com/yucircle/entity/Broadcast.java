package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("broadcast")
public class Broadcast {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private Long senderId;
    private String content;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
