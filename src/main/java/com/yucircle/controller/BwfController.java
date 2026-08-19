package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.service.BwfSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * BWF 数据查询 + 调试重抓入口。设计见 doc/06 §6。
 * 注意：当前 Security 全放行（本地调试），上生产需给 /bwf/admin/* 加 token。
 */
@RestController
@RequestMapping("/bwf")
@RequiredArgsConstructor
public class BwfController {

    private final BwfSyncService syncService;

    /** 最新一期排名（discipline 缺省 ms；ms/ws/md/wd/xd） */
    @GetMapping("/rankings")
    public ApiResponse<?> rankings(@RequestParam(defaultValue = "ms") String discipline) {
        return ApiResponse.success(syncService.latestRankings(discipline));
    }

    /** 单球员走势（本年，player 为姓名子串，如 SHI 或 石） */
    @GetMapping("/rankings/history")
    public ApiResponse<?> rankingHistory(@RequestParam String discipline,
                                         @RequestParam String player) {
        return ApiResponse.success(syncService.rankingHistory(discipline, player));
    }

    /** 本年赛程（含 code/tmtId/hasLiveScores） */
    @GetMapping("/schedule")
    public ApiResponse<?> schedule() {
        return ApiResponse.success(syncService.schedule());
    }

    /** 某赛事某日全部对阵（官方对阵顺序；date 缺省=当天） */
    @GetMapping("/matches")
    public ApiResponse<?> matches(@RequestParam int tmtId,
                                  @RequestParam(required = false) LocalDate date) {
        LocalDate d = date == null ? LocalDate.now() : date;
        return ApiResponse.success(syncService.matches(tmtId, d));
    }

    /** 单场详情（含逐局与逐分序列） */
    @GetMapping("/match-detail")
    public ApiResponse<?> matchDetail(@RequestParam int tmtId, @RequestParam String code) {
        Object detail = syncService.matchDetail(tmtId, code);
        if (detail == null) {
            return ApiResponse.error("场次不存在：" + tmtId + "/" + code);
        }
        return ApiResponse.success(detail);
    }

    /**
     * 手动重抓（调试主入口：数据不对→改代码→调这里→前端刷新）。
     * scope = all | rankings | schedule | matches（matches 可带 date，缺省当天）
     */
    @PostMapping("/admin/refresh")
    public ApiResponse<?> refresh(@RequestParam(defaultValue = "all") String scope,
                                  @RequestParam(required = false) LocalDate date)
            throws Exception {
        LocalDate d = date == null ? LocalDate.now() : date;
        StringBuilder msg = new StringBuilder();
        if ("all".equals(scope) || "rankings".equals(scope)) {
            msg.append("rankings ").append(syncService.syncRankings()).append(" 条; ");
        }
        if ("all".equals(scope) || "schedule".equals(scope)) {
            msg.append("schedule ").append(syncService.syncSchedule()).append(" 站; ");
        }
        if ("all".equals(scope) || "matches".equals(scope)) {
            msg.append("matches ").append(syncService.syncDayMatches(d)).append(" 场");
        }
        return ApiResponse.success(null, "同步完成: " + msg);
    }
}
