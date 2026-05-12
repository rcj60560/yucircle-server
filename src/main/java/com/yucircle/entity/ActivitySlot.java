package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_slot")
public class ActivitySlot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Integer courtNumber;
    private Integer slotNumber;
    private Long userId;
    private String slotStatus;
    private LocalDateTime lockExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
