# 06 · BWF 数据落盘子系统 — 架构设计

> 2026-08-19 设计定稿。目标：把羽联（BWF）公开数据抓取落库到 MySQL，供 App 读取与数据分析查表；本地先跑通，后续部署阿里云。
> 开发进度见 `07_BWF数据落盘_开发进度.md`。

## 1. 架构与数据流

```
extranet-lv.bwfbadminton.com（BWF 公开 JSON API）
        │  Java HttpClient（BwfExtranetClient，唯一网络出口）
        ▼
BwfSyncService（解析 + 幂等 upsert）◀── BwfSyncTasks（@Scheduled 定时）
        │                              ◀── POST /api/bwf/admin/refresh（手动重抓，调试用）
        ▼
MySQL · yucircle 库（5 张 bwf_* 表）
        │
        ▼
GET /api/bwf/*（查询接口）──▶ App 排名/赛程远端层（本地调试 localhost:8080，生产阿里云）
```

要点：
- **抓取全在 server 端**，上云后全自动，不依赖本机；工具箱（ky/tools bwf-data）保留作手动补抓与 GitHub raw 资产链路（App 兜底）。
- **App 的 live 赛况（当日对阵/比分页）继续直连 extranet-lv**，不经 server；server 库承担留存与分析。
- **幂等 upsert**：所有落库按唯一键 INSERT ... ON DUPLICATE KEY UPDATE，重抓即覆盖修正，不留脏数据。

## 2. 数据范围（v0 只做今年）

| 数据 | 范围 | 量级 |
|---|---|---|
| 赛程 bwf_tournament | 2026 年全部站点（World Tour + Grade 1 major） | ~32 行 |
| 排名快照 bwf_ranking_entry | 2026 年每周一期 × 5 单项 × Top50 | ~1.3 万行/年 |
| 单场赛果 bwf_match(+game) | 2026 年 has_live_scores 站点的当日赛果 | ~6000 场/年 |
| 逐分序列 bwf_match_point | **仅 Grade 1 大赛**（世锦赛/汤尤杯），其余站点不抓逐分 | ~5 万行/届 |

配置开关：`bwf.sync.point-sync-levels=major`（逗号分隔 level 列表，后续扩全站/回填他年只改配置，表结构不动）。

## 3. 数据源（extranet-lv 五个端点）

| 端点 | 用途 | 关键参数 |
|---|---|---|
| `/api/vue-rankingweek?rankId=2` | 排名期次（最新周 key 形如 `2026-34-4435`，**只认数字尾**） | rankId=2（世界排名；9 是 Race 榜，勿用） |
| `/api/vue-rankingtable` | 排名表 | rankId=2, catId=6..10(男单..混双), publicationId=数字尾, pageKey=50 |
| `/api/vue-grouped-year-tournaments?year=` | 全年赛程 | year |
| `/api/tournaments/day-matches` | 当日对阵 | tournamentCode=UUID, date, order=1, court=0 |
| `/api/h2h/match` | 单场逐分 | **tmt_id=数字赛事id（UUID 会 500）**, match_code |

坑位记录：
- 排名 `publicationId` 必须传周 key 的数字尾（`2026-34-4435` → `4435`），传整串返回空表。
- h2h 必须数字 `tmt_id`；day-matches 的响应行**不含**数字赛事 id，需从赛程表反查。
- day-matches 返回顺序即**官方对阵顺序**（not-before 赛制时间会漂移，不能按时间重排）。
- h2h 逐分数据 `team1/team2` 为**累计得分**（最后一条=局终分），不是 0/1 归属。
- Grade 1 分类名含 en-dash（`Grade 1 – Individual Tournaments`），匹配用子串 `"grade 1"` 且排除 `"junior"`。

## 4. 表设计（追加至 src/main/resources/init.sql）

| 表 | 关键列 | 唯一键（upsert 依据） |
|---|---|---|
| `bwf_tournament` | tmt_id, name, level, start_date, end_date, city, country, prize_money, code(UUID), has_live_scores | uk_tmt_id |
| `bwf_ranking_entry` | publication_date, discipline(ms..xd), rank, rank_change, country, player_name, points | uk_pub(discipline, publication_date, rank) |
| `bwf_match` | tmt_id, match_code, match_date, event, round_name, court_name, match_time, status(F/P/N), winner, duration_min, team1_country/team1_players/team1_seed, team2_*, score_text | uk_match(tmt_id, match_code) |
| `bwf_match_game` | match_id, game_no, team1_score, team2_score | uk_game(match_id, game_no) |
| `bwf_match_point` | game_id, ordering, team1, team2（累计分） | uk_point(game_id, ordering) |

球员 v0 存拼接文本（`A / B`），与 App 展示一致；规范化球员表（胜率/对阵分析）留后续。

## 5. 分层与文件清单

```
com.yucircle
├── util/BwfExtranetClient.java      # 5 端点封装；HTTP 可注入（测试 mock）
├── entity/BwfTournament|BwfRankingEntry|BwfMatch|BwfMatchGame|BwfMatchPoint.java
├── mapper/Bwf*Mapper.java            # MyBatis-Plus BaseMapper
├── service/BwfSyncService.java       # syncRankings/syncSchedule/syncDayMatches/syncMatchDetail + 查询
├── controller/BwfController.java     # 查询 + admin/refresh
└── task/BwfSyncTasks.java            # 定时入口
```

## 6. 接口设计（context-path /api）

| 方法/路径 | 说明 |
|---|---|
| GET `/bwf/rankings?discipline=ms` | 最新一期排名（默认男单） |
| GET `/bwf/rankings/history?discipline=ms&player=石宇奇` | 单球员多期走势（本年） |
| GET `/bwf/schedule` | 本年赛程（含 code/tmtId/hasLiveScores） |
| GET `/bwf/matches?tmtId=5601&date=2026-08-18` | 某赛事某日全部对阵（含比分） |
| GET `/bwf/match-detail?tmtId=5601&code=553` | 单场详情（含逐局与逐分序列） |
| POST `/bwf/admin/refresh?scope=all\|rankings\|schedule\|matches` | 手动重抓（调试；dev 关鉴权，生产 token） |

## 7. 定时策略（BwfSyncTasks，配置 bwf.sync.enabled）

| 任务 | 频率 | 内容 |
|---|---|---|
| weeklySync | 周一 10:00 | 排名（当周 publication）+ 全年赛程 |
| dailyMatchSync | 每日 22:00 | 进行中赛事的当日赛果；已结束场次按 point-sync-levels 抓逐分 |

## 8. App 侧切换（后端完成后）

`BwfDataService` 远端 URL 抽成可配置（dev=`http://localhost:8080/api/bwf/...`，生产=阿里云域名），GitHub raw 链路降级为第二兜底；live 数据不动。

## 9. 测试与部署

- 单测：BwfExtranetClient 注入 fixture JSON；BwfSyncService 覆盖解析正确性与**重抓幂等**（不翻倍）。
- 部署：本地 MySQL（现有配置）→ 阿里云仅换 datasource 环境变量（`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`）+ jar，零代码改动。
