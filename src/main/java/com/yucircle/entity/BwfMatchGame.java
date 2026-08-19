package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** BWF 局（唯一键 match_id+game_no） */
@Data
@TableName("bwf_match_game")
public class BwfMatchGame {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Integer gameNo;
    private Integer team1Score;
    private Integer team2Score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
