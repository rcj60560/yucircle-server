# 07 · BWF 数据落盘子系统 — 开发进度

> 持续更新：每完成一步本地提交一次并在此登记。架构与设计决策见 `06_BWF数据落盘_架构设计.md`。

## 开发步骤清单

- [x] 1. 文档体系：架构设计 + 本进度文档；清理过时交付文档
- [x] 2. 建表：init.sql 追加 5 张 `bwf_*` 表（含唯一键）+ entity/mapper
- [x] 3. BwfExtranetClient（5 端点、fetch 可注入）+ BwfParser 纯解析 + BwfParserTest 5 用例全绿（含官方顺序/累计分/grade1-major 坑位断言）；build.gradle 补 junit-platform-launcher
- [x] 4. BwfSyncService：syncRankings/syncSchedule/syncDayMatches/syncMatchPoints（JdbcTemplate 批量 ON DUPLICATE KEY UPDATE）+ 查询方法；bwf_match 增 order_no 列保官方顺序
- [x] 5. BwfController（rankings/history/schedule/matches/match-detail + admin/refresh 手动重抓）+ BwfSyncTasks（周一10:00 周更/每日22:00 日更，bwf.sync.enabled 开关）+ yml 配置段
- [x] 6. gradle build（编译+测试）全绿；全程分 6 次提交
- [x] 7. App 远端层切 server：debug 连 http://localhost:8080/api/bwf（未启动自动降级 GitHub raw→缓存→内置资产），release 仍走 GitHub raw

## 进度日志

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-08-19 | （见下） | 6/7. /bwf/app/* 资产同构端点 + WebCorsConfig（本地 web 调试跨域）+ App BwfDataService 按构建模式切源；双端测试全绿 |
| 2026-08-19 | （见下） | 5. 查询接口+手动重抓+定时任务+配置 |
| 2026-08-19 | （见下） | 4. 同步服务+查询；order_no 落官方顺序 |
| 2026-08-19 | （见下） | 3. 客户端+解析器+单测；测试基建修复（Gradle9 需 junit-platform-launcher）|
| 2026-08-19 | （见下） | 5 张 bwf_* 表 DDL（唯一键 upsert 依据）+ 5 entity + 5 mapper，compileJava 通过 |
| 2026-08-19 | — | 设计定稿（用户确认：v0 只今年、逐分仅 Grade 1 大赛、server 自抓、先本地后阿里云）；文档体系建立，旧交付文档清理（FINAL_REPORT / DELIVERY_* / PHASE4_COMPLETION_SUMMARY / API_TESTING / QUICKSTART / HELP 删除，README 与 doc/05 保留） |

## 下次接续

- 全部完成（2026-08-19）。联调入口：启动 server 后浏览器调 POST /api/bwf/admin/refresh?scope=all 落库，再 GET /api/bwf/app/rankings 验证；App debug 模式自动读 server。上生产：收紧 CORS 域名 + /bwf/admin/* 加 token。数据源坑位见架构文档 §3（publicationId 数字尾 / tmt_id 数字 / 官方顺序 / 累计分）。
- 常设约定：本地提交不 push；MySQL 本地 root/admin（yucircle 库）；App 与工具箱仓库见 circle 项目记忆。
