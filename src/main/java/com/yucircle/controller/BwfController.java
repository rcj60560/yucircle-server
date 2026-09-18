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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * 应用兼容端点：与 App 内置资产 JSON 同构（无 ApiResponse 包装），
     * 供 App 远端层直连（dev=localhost / 生产=阿里云）。
     */
    @GetMapping("/app/rankings")
    public Map<String, Object> appRankings() {
        Map<String, Object> disciplines = new LinkedHashMap<>();
        Map<String, String> names = Map.of("ms", "男单", "ws", "女单", "md", "男双", "wd", "女双", "xd", "混双");
        String updatedAt = null;
        for (String disc : new String[]{"ms", "ws", "md", "wd", "xd"}) {
            List<Map<String, Object>> entries = syncService.latestRankings(disc).stream()
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("rank", e.getRankNum()); // /app 契约对外仍叫 rank
                        m.put("change", e.getRankChange());
                        m.put("country", e.getCountry());
                        m.put("player", e.getPlayerName());
                        m.put("points", e.getPoints());
                        return m;
                    })
                    .toList();
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("name", names.get(disc));
            d.put("entries", entries);
            disciplines.put(disc, d);
            if (updatedAt == null && !entries.isEmpty()) {
                updatedAt = syncService.latestRankings(disc).get(0).getPublicationDate().toString();
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updatedAt", updatedAt);
        body.put("source", "yucircle-server");
        body.put("disciplines", disciplines);
        return body;
    }

    /** 应用兼容端点：赛程资产同构 */
    @GetMapping("/app/schedule")
    public Map<String, Object> appSchedule() {
        List<Map<String, Object>> tournaments = syncService.schedule().stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", t.getName());
                    m.put("startDate", t.getStartDate() == null ? null : t.getStartDate().toString());
                    m.put("endDate", t.getEndDate() == null ? null : t.getEndDate().toString());
                    m.put("city", t.getCity());
                    m.put("level", t.getLevel());
                    m.put("prizeMoney", t.getPrizeMoney() == null ? 0 : t.getPrizeMoney());
                    m.put("code", t.getCode());
                    m.put("tmtId", t.getTmtId());
                    m.put("hasLiveScores", Boolean.TRUE.equals(t.getHasLiveScores()));
                    return m;
                })
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updatedAt", LocalDate.now().toString());
        body.put("year", LocalDate.now().getYear());
        body.put("tournaments", tournaments);
        return body;
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
