# Phase 4 API 测试文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Response Format**: 所有接口返回 `ApiResponse<T>` 结构

### 统一响应格式
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

---

## 球馆接口

### 1. 获取球馆列表
```
GET /venues
```

**描述**: 返回所有球馆

**请求**: 无参数

**成功响应**:
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
      "distanceText": "未知距离",
      "onlineCount": 0,
      "hasActiveBroadcast": false
    }
  ]
}
```

**字段说明**:
- `id`: 球馆ID
- `name`: 球馆名称
- `address`: 地址
- `courtCount`: 场地数
- `distanceText`: 距离（占位）
- `onlineCount`: 在线人数
- `hasActiveBroadcast`: 是否有活跃广播

---

## 俱乐部接口

### 1. 获取某球馆的俱乐部列表
```
GET /clubs/venue/{venueId}
```

**参数**:
- `venueId`: 球馆ID（路径参数）

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "俱乐部 A",
      "description": "俱乐部 A 描述",
      "todayActivityCount": 1,
      "onlineCount": 0,
      "hasBroadcast": false,
      "status": "active"
    }
  ]
}
```

### 2. 创建俱乐部
```
POST /clubs
```

**请求体**:
```json
{
  "venueId": 1,
  "name": "新俱乐部",
  "description": "俱乐部描述"
}
```

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 4,
    "venueId": 1,
    "name": "新俱乐部",
    "creatorId": 1,
    "description": "俱乐部描述",
    "status": "active",
    "broadcastCredits": 0,
    "createdAt": "2026-05-12T19:30:00",
    "updatedAt": "2026-05-12T19:30:00"
  }
}
```

### 3. 获取俱乐部详情
```
GET /clubs/{clubId}
```

**参数**:
- `clubId`: 俱乐部ID（路径参数）

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "venueId": 1,
    "name": "俱乐部 A",
    "creatorId": 1,
    "description": "俱乐部 A 描述",
    "status": "active",
    "broadcastCredits": 0,
    "createdAt": "2026-05-12T00:00:00",
    "updatedAt": "2026-05-12T00:00:00"
  }
}
```

---

## 活动接口

### 1. 创建活动
```
POST /activities
```

**请求体**:
```json
{
  "venueId": 1,
  "clubId": 1,
  "title": "俱乐部 A 今晚活动",
  "activityDate": "2026-05-12",
  "timeSlot": "EVENING",
  "matchType": "DOUBLES",
  "courtCount": 3,
  "maxPerCourt": 6
}
```

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 4,
    "venueId": 1,
    "clubId": 1,
    "creatorId": 1,
    "title": "俱乐部 A 今晚活动",
    "activityDate": "2026-05-12",
    "timeSlot": "EVENING",
    "levelRequirement": null,
    "matchType": "DOUBLES",
    "courtCount": 3,
    "maxPerCourt": 6,
    "status": "open",
    "createdAt": "2026-05-12T19:30:00",
    "updatedAt": "2026-05-12T19:30:00"
  }
}
```

### 2. 进入活动大厅
```
POST /activities/{activityId}/enter
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**: 无

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "venueId": 1,
    "clubId": 1,
    "creatorId": 1,
    "title": "俱乐部 A 今晚活动",
    "status": "open"
  }
}
```

### 3. 获取大厅快照（核心接口）
```
GET /activities/{activityId}/lobby
```

**参数**:
- `activityId`: 活动ID（路径参数）

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "activityId": 1,
    "title": "俱乐部 A 今晚活动",
    "venueName": "晴天羽毛球馆",
    "clubName": "俱乐部 A",
    "timeSlot": "EVENING",
    "status": "open",
    "onlineCount": 5,
    "broadcast": {
      "id": 1,
      "content": "1号场高质量缺人，L4以上速来",
      "expiresAt": "2026-05-12T21:00:00"
    },
    "courts": [
      {
        "courtNumber": 1,
        "status": "recruiting",
        "confirmedCount": 2,
        "slots": [
          {
            "slotNumber": 1,
            "userId": 1001,
            "nickname": "张三",
            "avatar": "/avatars/a.png",
            "level": "L4",
            "status": "confirmed"
          },
          {
            "slotNumber": 2,
            "userId": 1002,
            "nickname": "李四",
            "avatar": "/avatars/b.png",
            "level": "L3",
            "status": "reserved"
          }
        ]
      }
    ],
    "observers": [
      {
        "userId": 1003,
        "nickname": "王五",
        "avatar": "/avatars/c.png",
        "level": "L2"
      }
    ],
    "messages": [
      {
        "id": 100,
        "nickname": "张三",
        "level": "L4",
        "content": "1号场差一个人，来不？",
        "createdAt": "19:40:30"
      }
    ]
  }
}
```

### 4. 心跳保活
```
POST /activities/{activityId}/ping
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**: 无

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 5. 离开大厅
```
POST /activities/{activityId}/leave
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**: 无

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 6. 占坑
```
POST /activities/{activityId}/reserve
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**:
```json
{
  "courtNumber": 1,
  "slotNumber": 1
}
```

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "slotId": 101,
    "courtNumber": 1,
    "slotNumber": 1,
    "userId": 1001,
    "nickname": "张三",
    "avatar": "/avatars/a.png",
    "level": "L4",
    "status": "reserved"
  }
}
```

**失败响应**:
```json
{
  "code": 500,
  "msg": "User already has a slot in this activity",
  "data": null
}
```

### 7. 取消占坑
```
DELETE /activities/{activityId}/reserve
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**: 无

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 8. 确认参加
```
POST /activities/{activityId}/confirm
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**: 无

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "slotId": 101,
    "courtNumber": 1,
    "slotNumber": 1,
    "userId": 1001,
    "nickname": "张三",
    "avatar": "/avatars/a.png",
    "level": "L4",
    "status": "confirmed"
  }
}
```

**失败响应** (如果用户没有坑位):
```json
{
  "code": 500,
  "msg": "User has no slot in this activity",
  "data": null
}
```

### 9. 发送聊天消息
```
POST /activities/{activityId}/messages
```

**参数**:
- `activityId`: 活动ID（路径参数）

**请求体**:
```json
{
  "content": "1号场差一个人，来不？"
}
```

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 100,
    "nickname": "张三",
    "level": "L4",
    "content": "1号场差一个人，来不？",
    "createdAt": "19:40:30"
  }
}
```

**失败响应** (消息限频):
```json
{
  "code": 500,
  "msg": "Message rate limit: 1 message per 60 seconds",
  "data": null
}
```

---

## 广播接口

### 1. 获取活跃广播
```
GET /broadcasts/active?clubId={clubId}
```

**参数**:
- `clubId`: 俱乐部ID（查询参数）

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "content": "1号场高质量缺人，L4以上速来",
      "expiresAt": "2026-05-12T21:00:00"
    }
  ]
}
```

### 2. 发送广播
```
POST /broadcasts/clubs/{clubId}
```

**参数**:
- `clubId`: 俱乐部ID（路径参数）

**请求体**:
```json
{
  "content": "1号场高质量缺人，L4以上速来",
  "expiresInSeconds": 3600
}
```

**成功响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 2,
    "clubId": 1,
    "senderId": 1,
    "content": "1号场高质量缺人，L4以上速来",
    "expiresAt": "2026-05-12T21:00:00",
    "createdAt": "2026-05-12T20:00:00"
  }
}
```

---

## 关键业务规则

### 坑位状态流转
```
empty → reserved (占坑) → confirmed (确认) → locked (成局)
     ↓
     ← cancel (取消)
```

- **empty**: 坑位空闲
- **reserved**: 用户已占坑，3分钟后自动释放
- **confirmed**: 用户已确认参加
- **locked**: 该场地6人全部确认时，所有坑位变为 locked

### 聊天消息限频
- 同一用户 60 秒内最多发 1 条消息
- 消息自动保留 1 ���，之后自动删除

### 在线状态管理
- 用户进入大厅时创建 presence 记录
- 客户端需要定时 ping（建议 30 秒内一次）
- 服务端 30 秒内未收到 ping 自动移除用户

### 广播
- 需要指定过期时间（expiresInSeconds）
- 过期后自动删除

---

## 测试流程

### 完整流程
1. **获取球馆**: `GET /venues`
2. **获取俱乐部**: `GET /clubs/venue/1`
3. **进入大厅**: `POST /activities/1/enter`
4. **查看大厅快照**: `GET /activities/1/lobby`
5. **占坑**: `POST /activities/1/reserve`，body: `{"courtNumber": 1, "slotNumber": 1}`
6. **确认参加**: `POST /activities/1/confirm`
7. **发送消息**: `POST /activities/1/messages`，body: `{"content": "test"}`
8. **心跳**: `POST /activities/1/ping`
9. **离开**: `POST /activities/1/leave`

### 预期结果
- ✅ 能正常获取球馆和俱乐部
- ✅ 能进入大厅并查看��照
- ✅ 能占坑、确认
- ✅ 6人全部确认后，场地状态变为 locked
- ✅ 消息 60 秒限频生效
- ✅ 离线状态正确管理

---

## 注意事项

1. **userId**: 当前所有接口都使用硬编码的 `userId = 1L`，后续需要从 JWT 令牌中获取
2. **错误处理**: 当前错误响应直接返回 500，建议后续优化为更细化的错误码
3. **权限**: 当前未做权限校验，建议添加用户认证和操作权限检查
4. **性能**: 大厅快照接口涉及多表查询，可考虑后续优化或缓存

---

## 示例 Postman Collection

可在 `postman_collection.json` 中导入以下集合来快速测试所有接口。

