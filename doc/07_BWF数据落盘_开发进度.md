# 07 · BWF 数据落盘子系统 — 开发进度

> 持续更新：每完成一步本地提交一次并在此登记。架构与设计决策见 `06_BWF数据落盘_架构设计.md`。

## 开发步骤清单

- [x] 1. 文档体系：架构设计 + 本进度文档；清理过时交付文档
- [ ] 2. 建表：init.sql 追加 5 张 `bwf_*` 表（含唯一键）+ entity/mapper
- [ ] 3. BwfExtranetClient：5 端点封装（HTTP 可注入）+ 单测
- [ ] 4. BwfSyncService：解析 + 幂等 upsert（逐分仅 major）+ 单测
- [ ] 5. BwfController 查询接口 + admin/refresh + BwfSyncTasks 定时 + yml 配置
- [ ] 6. gradle 编译/测试全绿，分次提交
- [ ] 7. （后端完成后）App 远端层切 server（dev=localhost），前端提交

## 进度日志

| 日期 | 提交 | 内容 |
|---|---|---|
| 2026-08-19 | — | 设计定稿（用户确认：v0 只今年、逐分仅 Grade 1 大赛、server 自抓、先本地后阿里云）；文档体系建立，旧交付文档清理（FINAL_REPORT / DELIVERY_* / PHASE4_COMPLETION_SUMMARY / API_TESTING / QUICKSTART / HELP 删除，README 与 doc/05 保留） |

## 下次接续

- 当前步骤：见清单打勾处；数据源坑位见架构文档 §3（publicationId 数字尾 / tmt_id 数字 / 官方顺序 / 累计分）。
- 常设约定：本地提交不 push；MySQL 本地 root/admin（yucircle 库）；App 与工具箱仓库见 circle 项目记忆。
