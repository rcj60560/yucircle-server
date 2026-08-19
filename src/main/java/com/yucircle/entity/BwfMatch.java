package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** BWF 单场赛果（唯一键 tmt_id+match_code） */
@Data
@TableName("bwf_match")
public class BwfMatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer tmtId;
    private String matchCode;
    private LocalDate matchDate;
    private String event;
    private String roundName;
    private String courtName;
    private LocalDateTime matchTime;
    /** F=已结束 P=进行中 N=未开赛（对应 extranet matchStatus） */
    private String status;
    private Integer winner;
    private Integer durationMin;
    private String team1Country;
    private String team1Players;
    private String team1Seed;
    private String team2Country;
    private String team2Players;
    private String team2Seed;
    private String scoreText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
