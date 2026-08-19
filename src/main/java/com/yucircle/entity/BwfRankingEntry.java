package com.yucircle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** BWF 排名快照明细（唯一键 discipline+publication_date+rank） */
@Data
@TableName("bwf_ranking_entry")
public class BwfRankingEntry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String discipline;
    private LocalDate publicationDate;
    private Integer rank;
    private Integer rankChange;
    private String country;
    private String playerName;
    private Integer points;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
