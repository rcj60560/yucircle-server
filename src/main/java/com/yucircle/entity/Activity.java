package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long venueId;
    private Long clubId;
    private Long creatorId;
    private String title;
    private LocalDate activityDate;
    private String timeSlot;
    private String levelRequirement;
    private String matchType;
    private Integer courtCount;
    private Integer maxPerCourt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
