package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("venue")
public class Venue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String city;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer courtCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
