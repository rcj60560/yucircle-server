package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** BWF 逐分（唯一键 game_id+ordering；team1/team2 为累计得分） */
@Data
@TableName("bwf_match_point")
public class BwfMatchPoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long gameId;
    private Integer ordering;
    private Integer team1;
    private Integer team2;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
