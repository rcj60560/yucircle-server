package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("club")
public class Club {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long venueId;
    private String name;
    private Long creatorId;
    private String description;
    private String status;
    private Integer broadcastCredits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
