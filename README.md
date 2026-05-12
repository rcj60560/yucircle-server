# YuCircle - Phase 4 服务端系统

## 📖 项目介绍

YuCircle 是一个**球馆俱乐部活动大厅系统**的后端服务，用于管理球馆、俱乐部、活动和用户占坑。

**当前阶段**: Phase 4 - 球馆俱��部活动大厅系统  
**开发状态**: ✅ 完全完成  
**版本**: 1.0.0

---

## 🚀 快速开始

### 1. 环境要求
- JDK 17+
- MySQL 5.7+
- Gradle 9.4+（或使用项目内 gradlew）

### 2. 数据库初始化
```bash
mysql -u root -p < src/main/resources/init.sql
```

### 3. 编译
```bash
./gradlew clean build -x test
```

### 4. 启动
```bash
java -jar build/libs/yucircle-server-1.0.0.jar
```

### 5. 测试
```bash
# 在另一个终端执行
curl http://localhost:8080/api/venues
```

更详细的步骤请查看 [QUICKSTART.md](QUICKSTART.md)。

---

## 📚 文档指南

| 文档 | 用途 |
|------|------|
| [QUICKSTART.md](QUICKSTART.md) | 30分钟快速上手 |
| [API_TESTING.md](API_TESTING.md) | 13个接口的完整说明和示例 |
| [PHASE4_COMPLETION_SUMMARY.md](PHASE4_COMPLETION_SUMMARY.md) | 项目功能总结 |
| [DELIVERY_SUMMARY.md](DELIVERY_SUMMARY.md) | 完整的交付总结 |
| [DELIVERY_CHECKLIST.md](DELIVERY_CHECKLIST.md) | 交付物清单 |
| [FINAL_REPORT.md](FINAL_REPORT.md) | **最终交付报告** ⭐ |

**建议阅读顺序**:
1. 本文件 (README.md) - 了解项目
2. QUICKSTART.md - 快速部署
3. API_TESTING.md - 接口测试
4. FINAL_REPORT.md - 完整总结

---

## 🏗️ 系统架构

```
球馆 (Venue)
  ↓
俱乐部 (Club)
  ├─→ 活动 (Activity)
  │   ├─→ 场地 (CourtDto)
  │   │   └─→ 坑位 (ActivitySlot)
  │   ├─→ 在线状态 (ActivityPresence)
  │   ├─→ 聊天消息 (ActivityMessage)
  │   └─→ 观察者 (ObserverDto)
  └─→ 广播 (Broadcast)
```

---

## 📊 核心数据表

| 表名 | 用途 | 记录数 |
|------|------|--------|
| venue | 球馆 | 1 |
| club | 俱乐部 | 3 |
| activity | 活动 | 3 |
| activity_slot | 坑位 | 54 |
| activity_presence | 在线状态 | 可变 |
| activity_message | 聊天消息 | 可变 |
| broadcast | 广播 | 可变 |

---

## 🎯 13 个核心 API 接口

### 球馆 (1)
- `GET /venues` - 获取球馆列表

### 俱乐部 (3)
- `GET /clubs/venue/{venueId}` - 获取俱乐部列表
- `POST /clubs` - 创建俱乐部
- `GET /clubs/{clubId}` - 获取俱乐部详情

### 活动 (9)
- `POST /activities` - 创建活动
- `POST /activities/{activityId}/enter` - 进入大厅
- `GET /activities/{activityId}/lobby` - **获取大厅快照【核心】**
- `POST /activities/{activityId}/ping` - 心跳保活
- `POST /activities/{activityId}/leave` - 离开大厅
- `POST /activities/{activityId}/reserve` - 占坑
- `DELETE /activities/{activityId}/reserve` - 取消占坑
- `POST /activities/{activityId}/confirm` - 确认参加
- `POST /activities/{activityId}/messages` - 发送聊天消息

### 广播 (2)
- `GET /broadcasts/active?clubId={clubId}` - 获取活跃广播
- `POST /broadcasts/clubs/{clubId}` - 发送广播

---

## 🔑 核心业务规则

### 坑位状态流转
```
empty (空闲)
  ↓
reserved (已占坑，3分钟后自动释放)
  ↓
confirmed (已确认参加)
  ↓
locked (6人都确认后，该场地锁定)
```

### 关键规则
- ✅ 一个用户在同一活动中只能占 1 个坑
- ✅ 占坑后需要确认才算参加
- ✅ 6 人确认后该场地自动锁定
- ✅ 聊天消息限频：60秒1条
- ✅ 在线超过 30 秒无 ping 自动离线
- ✅ 广播支持有效期设置

---

## 🧪 完整测试流程

```bash
# 1. 获取球馆
curl http://localhost:8080/api/venues

# 2. 获取俱乐部
curl http://localhost:8080/api/clubs/venue/1

# 3. 进入活动
curl -X POST http://localhost:8080/api/activities/1/enter

# 4. 查看大厅快照
curl http://localhost:8080/api/activities/1/lobby

# 5. 占坑
curl -X POST http://localhost:8080/api/activities/1/reserve \
  -H "Content-Type: application/json" \
  -d '{"courtNumber": 1, "slotNumber": 1}'

# 6. 确认参加
curl -X POST http://localhost:8080/api/activities/1/confirm

# 7. 发送消息
curl -X POST http://localhost:8080/api/activities/1/messages \
  -H "Content-Type: application/json" \
  -d '{"content": "我已确认参加！"}'

# 8. 心跳保活
curl -X POST http://localhost:8080/api/activities/1/ping

# 9. 离开大厅
curl -X POST http://localhost:8080/api/activities/1/leave
```

详细请查看 [QUICKSTART.md](QUICKSTART.md) 中的"完整测试流程"部分。

---

## 📦 代码���构

```
src/main/java/com/yucircle/
├── entity/              (7个Entity类)
├── mapper/              (7个Mapper接口)
├── service/             (8个Service类)
│   └── impl/
├── controller/          (4个Controller，13个接口)
├── dto/                 (14个DTO类)
├── task/                (定时任务)
└── YuCircleApplication.java
```

**代码行数**: ~1,900 行  
**编译状态**: ✅ BUILD SUCCESSFUL  
**编译耗时**: 14 秒

---

## ⚙️ 定时任务

系统内置 4 个自动清理任务：

| 任务 | 频率 | 作用 |
|------|------|------|
| cleanupExpiredPresence | 每10秒 | 清理30秒无ping的在线记录 |
| cleanupExpiredSlots | 每30秒 | 清理3分钟未确认的坑位 |
| cleanupOldMessages | 每1小时 | 清理前一天的聊天消息 |
| cleanupExpiredBroadcasts | 每5分钟 | 清理过期广播 |

所有任务自动执行，无需手动干预。

---

## 🔧 常见问题

### Q: ���何修改数据库连接？
**A**: 修改 `src/main/resources/application.yml`:
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/yucircle
  username: root
  password: admin
```

### Q: 如何修改启动端口？
**A**: 修改 `src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # 改为其他端口
```

### Q: 占坑失败提示"User already has a slot"
**A**: 用户在同一活动中只能占1个坑。取消当前坑位后再试：
```bash
curl -X DELETE http://localhost:8080/api/activities/1/reserve
```

### Q: 消息发送失败提示"Message rate limit"
**A**: 消息限频为60秒1条。请稍候再试。

更多问题请查看 [QUICKSTART.md](QUICKSTART.md) 中的"常见问题"部分。

---

## 📈 性能指标

| 指标 | 数值 |
|------|------|
| 代码量 | ~1,900 行 |
| Entity 类数 | 7 个 |
| API 接口数 | 13 个 |
| 数据表数 | 7 张 |
| 定时任务数 | 4 个 |
| 编译耗时 | 14 秒 |
| JAR 包大小 | 30 MB |

---

## ✅ 交付清单

- [x] 7 张数据表 + 测试数据
- [x] 7 个 Entity 类
- [x] 7 个 Mapper 接口
- [x] 8 个 Service 类
- [x] 13 个 API 接口
- [x] 14 个 DTO 类
- [x] 4 个定时清理任务
- [x] 完整的代码编译
- [x] 可执行 JAR 包
- [x] 5 份详细文档

---

## 🚀 下一步

### 立即可做
1. 部署应用 → 查看 [QUICKSTART.md](QUICKSTART.md)
2. 测试接口 → 查看 [API_TESTING.md](API_TESTING.md)
3. 集成前端 → 提供 API 文档

### 短期改进
- 从 JWT 获取 userId（当前硬编码）
- 添加权限校验
- 优化大厅快照性能
- 添加详细日志

### 长期规划
- 支持多球馆和多时间段
- 集成 Redis 缓存
- WebSocket 推送实时更新
- 更复杂的业务规则

---

## 📝 项目信息

- **项目名**: YuCircle - 球馆俱乐部活动大厅系统
- **开发阶段**: Phase 4
- **版本**: 1.0.0
- **完成日期**: 2026-05-12
- **技术栈**: Spring Boot 3.3.5 + MyBatis-Plus 3.5.9 + MySQL
- **编译状态**: ✅ BUILD SUCCESSFUL
- **部署就绪**: ✅ YES

---

## 📞 联系方式

如有任何问题或需要支持，请查阅相应文档：

- **快速开始**: [QUICKSTART.md](QUICKSTART.md)
- **API 文档**: [API_TESTING.md](API_TESTING.md)
- **功能总结**: [PHASE4_COMPLETION_SUMMARY.md](PHASE4_COMPLETION_SUMMARY.md)
- **完整说明**: [DELIVERY_SUMMARY.md](DELIVERY_SUMMARY.md)
- **最终报告**: [FINAL_REPORT.md](FINAL_REPORT.md)

---

## 📄 许可证

本项目仅供内部使用。

---

**祝开发顺利！** 🎉

