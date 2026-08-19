package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** BWF 赛事（唯一键 tmt_id，重抓 upsert） */
@Data
@TableName("bwf_tournament")
public class BwfTournament {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer tmtId;
    private String name;
    private String level;
    private LocalDate startDate;
    private LocalDate endDate;
    private String city;
    private String country;
    private Integer prizeMoney;
    private String code;
    private Boolean hasLiveScores;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
