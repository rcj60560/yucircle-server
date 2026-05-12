# Phase 4 快速开始指南

## 🚀 快速部署

### 前置要求
- JDK 17+
- MySQL 5.7+
- Gradle 9.4+（或使用项目自带的 gradlew）

### 1. 数据库初始化
```bash
# 方式一：使用 MySQL 命令行
mysql -u root -p < src/main/resources/init.sql

# 方式二：通过 MySQL 图形工具
# 打开 src/main/resources/init.sql，复制内容到图形工具执行
```

### 2. 编译项目
```bash
# Windows
.\gradlew clean build -x test

# Linux/Mac
./gradlew clean build -x test
```

编译成功后，JAR 包位置：`build/libs/yucircle-server-1.0.0.jar`

### 3. 启动应用
```bash
java -jar build/libs/yucircle-server-1.0.0.jar
```

成功启动后，控制台会显示：
```
Started YuCircleApplication in X.XXX seconds
```

### 4. 验证应用
```bash
# 在另一个终端执行
curl http://localhost:8080/api/venues
```

预期返回：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "晴天羽毛球馆",
      "address": "成都市武侯区",
      "courtCount": 10,
      ...
    }
  ]
}
```

---

## 📱 常用 API 示例

### 1. 获取球馆列表
```bash
curl -X GET http://localhost:8080/api/venues
```

### 2. 获取俱乐部列表
```bash
curl -X GET http://localhost:8080/api/clubs/venue/1
```

### 3. 获取活动大厅快照（核心接口）
```bash
curl -X GET http://localhost:8080/api/activities/1/lobby
```

### 4. 进入活动大厅
```bash
curl -X POST http://localhost:8080/api/activities/1/enter
```

### 5. 占坑
```bash
curl -X POST http://localhost:8080/api/activities/1/reserve \
  -H "Content-Type: application/json" \
  -d '{"courtNumber": 1, "slotNumber": 1}'
```

### 6. 确认参加
```bash
curl -X POST http://localhost:8080/api/activities/1/confirm
```

### 7. 发送消息
```bash
curl -X POST http://localhost:8080/api/activities/1/messages \
  -H "Content-Type: application/json" \
  -d '{"content": "1号场差一个人，来不？"}'
```

### 8. 发送广播
```bash
curl -X POST http://localhost:8080/api/broadcasts/clubs/1 \
  -H "Content-Type: application/json" \
  -d '{"content": "1号场高质量缺人，L4以上速来", "expiresInSeconds": 3600}'
```

---

## 🧪 完整测试流程

### 场景：用户参加活动

```bash
# 1. 查看球馆
curl http://localhost:8080/api/venues

# 2. 查看俱乐部
curl http://localhost:8080/api/clubs/venue/1

# 3. 进入活动
curl -X POST http://localhost:8080/api/activities/1/enter

# 4. 查看大厅快照
curl http://localhost:8080/api/activities/1/lobby

# 5. 占坑（1号场1号坑）
curl -X POST http://localhost:8080/api/activities/1/reserve \
  -H "Content-Type: application/json" \
  -d '{"courtNumber": 1, "slotNumber": 1}'

# 6. 再查看大厅（应该看到自己的坑位状态变为 reserved）
curl http://localhost:8080/api/activities/1/lobby

# 7. 确认参加
curl -X POST http://localhost:8080/api/activities/1/confirm

# 8. 再查看大厅（坑位状态应该变为 confirmed）
curl http://localhost:8080/api/activities/1/lobby

# 9. 发送消息
curl -X POST http://localhost:8080/api/activities/1/messages \
  -H "Content-Type: application/json" \
  -d '{"content": "我已确认参加！"}'

# 10. 再查看大厅（应该看到消息）
curl http://localhost:8080/api/activities/1/lobby

# 11. 离开大厅
curl -X POST http://localhost:8080/api/activities/1/leave
```

---

## 📊 监控定时任务

定时任务自动执行，无需手动干预：

| 任务 | 频率 | 作用 |
|------|------|------|
| cleanupExpiredPresence | 每 10 秒 | 清理 30 秒无 ping 的在线记录 |
| cleanupExpiredSlots | 每 30 秒 | 清理 3 分钟未确认的坑位 |
| cleanupOldMessages | 每 1 小时 | 清理前一天的聊天消息 |
| cleanupExpiredBroadcasts | 每 5 分钟 | 清理过期的广播 |

查看任务执行：在应用启动后的控制台日志中可以看到任务执行记录。

---

## 🔧 常见问题

### Q: 连接数据库失败
**A**: 检查 MySQL 服务是否启动，确认 `src/main/resources/application.yml` 中的数据库配置：
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/yucircle?useUnicode=true&characterEncoding=utf-8
  username: root
  password: admin
```

### Q: 端口 8080 被占用
**A**: 修改 `application.yml`：
```yaml
server:
  port: 8081  # 改为其他端口
```

### Q: 占坑失败提示"User already has a slot"
**A**: 这是正常的。用户在同一活动中只能占 1 个坑。需要先取消当前坑位：
```bash
curl -X DELETE http://localhost:8080/api/activities/1/reserve
```

### Q: 消息发送失败提示"Message rate limit"
**A**: 这是限频机制。用户 60 秒内最多发 1 条消息，请稍候再试。

### Q: 坑位没有自动释放
**A**: 有两种情况：
1. 坑位为 "confirmed" 状态，不会自动释放
2. 坑位为 "reserved" 状态，超过 3 分钟后自动释放（由定时任务处理）

---

## 📚 文档指引

- **项目概览**: 查看 `PHASE4_COMPLETION_SUMMARY.md`
- **API 详细文档**: 查看 `API_TESTING.md`
- **完整交付总结**: 查看 `DELIVERY_SUMMARY.md`
- **SQL 脚本**: 查看 `src/main/resources/init.sql`

---

## 🎯 验证清单

启动应用后，依次验证以下项目：

- [ ] 应用成功启动（端口 8080）
- [ ] GET /venues 返回 1 个球馆
- [ ] GET /clubs/venue/1 返回 3 个俱乐部
- [ ] GET /activities/1/lobby 返回大厅快照（包括场地、坑位、消息等）
- [ ] POST /activities/1/reserve 可成功占坑
- [ ] POST /activities/1/confirm 可成功确认
- [ ] 6 人确认后，场地状态变为 "locked"
- [ ] 消息 60 秒限频生效
- [ ] 广播能查询和过期

---

## 💡 开发建议

### 短期（当前）
1. 用 Postman 或 curl 测试所有接口
2. 验证数据库初始化和定时任务
3. 检查边界场景和错误处理

### 中期（1-2周）
1. 集成用户认证（从 JWT 获取 userId）
2. 添加权限校验
3. 优化大厅快照性能（考虑缓存）
4. 添加更详细的日志

### 长期（后续）
1. 支持多球馆和多时间段
2. 集成搜索和筛选功能
3. 添加缓存层（Redis）
4. 支持更复杂的权限模型

---

## 📞 获取帮助

### 查看日志
应用运行时的所有日志都输出到控制台，包括：
- 定时任务执行情况
- 数据库查询日志（MyBatis-Plus 配置）
- Spring Boot 启动信息

### 检查数据库
```bash
# 连接到 MySQL
mysql -u root -p yucircle

# 查看表
SHOW TABLES;

# 查看某个表的内容
SELECT * FROM venue;
SELECT * FROM activity;
SELECT * FROM activity_slot;
```

### 查看项目结构
```
yucircle-server/
├── src/main/java/com/yucircle/
│   ├── entity/       (7 个 Entity 类)
│   ���── mapper/       (7 个 Mapper 接口)
│   ├── service/      (4 个 Service 实现)
│   ├── controller/   (4 个 Controller，13 个接口)
│   ├── dto/          (14 个 DTO 类)
│   ├── task/         (定时任务)
│   └── YuCircleApplication.java
├── src/main/resources/
│   ├── init.sql      (数据库初始化脚本)
│   ├── application.yml
│   └── application.properties
└── build.gradle
```

---

## ✨ 项目特色

✅ **完整的业务流程** - 从球馆到坑位的全链路实现  
✅ **规范的代码结构** - 严格分层设计，易于维护  
✅ **自��化清理** - 定时任务自动处理过期数据  
✅ **灵活的状态设计** - 支持复杂的业务场景  
✅ **充分的文档** - 包括 API 文档和部署指南  

---

祝部署顺利！🎉

