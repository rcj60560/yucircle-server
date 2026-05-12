# Phase 4：球馆 / 俱乐部 / 活动大厅系统服务端开发文档

## 文档目标

本文档只面向 Spring Boot 服务端开发，约束 Phase 4 需要新增的数据模型、接口、状态流转和初始化数据。

客户端页面和交互说明已拆分到 App 项目中，本服务端文档只关注后端应当交付什么。

---

## 一、业务目标

Phase 4 的目标是把“论坛互动”扩展为“可成局的打球组织系统”。

本阶段的组织结构固定为：

```text
球馆 Venue
  -> 俱乐部 Club
      -> 活动大厅 Activity
          -> 场地坑位 ActivitySlot
          -> 在线状态 ActivityPresence
          -> 聊天消息 ActivityMessage

俱乐部 Club
  -> 广播 Broadcast
```

当前 MVP 固定测试数据：

- 城市：成都
- 球馆：晴天羽毛球馆
- 场地数：10
- 俱乐部：俱乐部 A / 俱乐部 B / 俱乐部 C
- 时间段：今晚 19:30 - 22:00
- 每场人数：6 人

---

## 二、服务端必须支持的业务能力

### 2.1 球馆与俱乐部

1. 返回球馆列表
2. 返回某球馆下的俱乐部列表
3. 支持俱乐部创建申请
4. 俱乐部状态需支持审核和冻结预留

### 2.2 活动大厅

1. 支持俱乐部活动大厅
2. 支持个人发起活动，`club_id` 可为空
3. 用户进入大厅后进入观望状态
4. 支持占坑
5. 支持取消占坑
6. 支持确认参加
7. 当某场地 6 人全部确认时，该场地进入已成局状态
8. 服务端返回大厅完整快照，供客户端轮询渲染

### 2.3 广播与聊天

1. 普通用户只能发大厅聊天消息
2. 俱乐部可发广播消息
3. 聊天消息限频：60 秒 1 条
4. 广播需支持有效期
5. 聊天消息只保留当天

### 2.4 在线状态

1. 用户进入大厅后写入在线记录
2. 客户端每隔一段时间发送 ping
3. 超过 30 秒无 ping 视为离线
4. 离线用户从在线列表移除
5. 已占坑用户掉线后先保留坑位 3 分钟，再由任务释放

---

## 三、数据表设计

建议统一迁移目录为：

```text
src/main/resources/db/migration/
```

本阶段新增脚本建议命名为：

```text
V3__create_venue_club_activity_tables.sql
```

### 3.1 venue

字段建议：

- id
- name
- city
- address
- latitude
- longitude
- court_count
- status
- created_at
- updated_at

初始化数据：

- 晴天羽毛球馆
- city = 成都
- court_count = 10

### 3.2 club

字段建议：

- id
- venue_id
- name
- creator_id
- description
- status
- broadcast_credits
- created_at
- updated_at

状态建议：

- pending
- active
- frozen
- deleted

初始化数据：

- 俱乐部 A
- 俱乐部 B
- 俱乐部 C

### 3.3 activity

字段建议：

- id
- venue_id
- club_id，可为空
- creator_id
- title
- activity_date
- time_slot
- level_requirement
- match_type
- court_count
- max_per_court
- status
- created_at
- updated_at

建议值：

- time_slot = EVENING
- match_type = DOUBLES
- max_per_court = 6

状态建议：

- open
- recruiting
- waiting_confirm
- locked
- finished
- canceled

### 3.4 activity_slot

字段建议：

- id
- activity_id
- court_number
- slot_number
- user_id
- slot_status
- lock_expires_at
- created_at
- updated_at

状态建议：

- reserved
- confirmed
- locked

约束：

1. `(activity_id, court_number, slot_number)` 唯一
2. `(activity_id, user_id)` 唯一，防止一人多坑

### 3.5 activity_presence

字段建议：

- id
- activity_id
- user_id
- last_ping_at
- created_at

约束：

1. `(activity_id, user_id)` 唯一

### 3.6 activity_message

字段建议：

- id
- activity_id
- user_id
- content
- created_at

### 3.7 broadcast

字段建议：

- id
- club_id
- sender_id
- content
- expires_at
- created_at

---

## 四、Java 代码结构要求

### 4.1 Entity

在 `src/main/java/com/yucircle/entity/` 下新增：

1. Venue.java
2. Club.java
3. Activity.java
4. ActivitySlot.java
5. ActivityPresence.java
6. ActivityMessage.java
7. Broadcast.java

要求：

1. 延续现有 `Post.java`、`User.java` 风格
2. 使用 MyBatis-Plus 的 `@TableName`
3. 主键继续使用 `@TableId(type = IdType.AUTO)`
4. 字段命名遵循下划线映射驼峰

### 4.2 Mapper

在 `src/main/java/com/yucircle/mapper/` 下新增对应 Mapper。

### 4.3 DTO

在 `src/main/java/com/yucircle/dto/` 下新增：

1. VenueDto
2. ClubDto
3. ActivityLobbyDto
4. CourtDto
5. SlotDto
6. ObserverDto
7. ActivityMessageDto
8. BroadcastDto
9. CreateClubRequest
10. CreateActivityRequest
11. ReserveSlotRequest
12. SendBroadcastRequest
13. SendActivityMessageRequest

---

## 五、Service 层任务

### 5.1 VenueService

职责：

1. 初始化 MVP 球馆数据
2. 返回球馆列表
3. 汇总球馆维度在线人数和广播概览

### 5.2 ClubService

职责：

1. 查询某球馆下俱乐部列表
2. 创建俱乐部申请
3. 返回俱乐部详情
4. 统计俱乐部今日活动数和在线人数

### 5.3 ActivityService

职责：

1. 创建活动
2. 用户进入大厅
3. 获取大厅快照
4. 占坑
5. 取消占坑
6. 确认参加
7. 发送大厅消息
8. 用户离开大厅
9. 心跳保活
10. 计算场地状态和活动状态

关键规则：

1. 用户进入大厅默认进入观望区
2. 一个用户在同一活动中只能占 1 个坑
3. 占坑后再次确认，才算 confirmed
4. 某场地 6 人都 confirmed，整场地进入 locked
5. locked 场地不允许继续抢坑

### 5.4 BroadcastService

职责：

1. 查询当前有效广播
2. 发送俱乐部广播
3. 校验广播权限
4. 扣减广播次数预留

---

## 六、Controller 与 API 设计

所有接口继续使用现有统一返回结构 `ApiResponse<T>`，接口风格参照当前 `PostController`。

### 6.1 VenueController

- `GET /venues`

返回球馆列表，列表中建议直接带出：

- id
- name
- address
- courtCount
- distanceText，占位字段即可
- onlineCount
- hasActiveBroadcast

### 6.2 ClubController

- `GET /venues/{venueId}/clubs`
- `POST /clubs`
- `GET /clubs/{clubId}`

俱乐部列表建议直接带出：

- id
- name
- description
- todayActivityCount
- onlineCount
- hasBroadcast
- status

### 6.3 ActivityController

- `POST /activities`
- `POST /activities/{activityId}/enter`
- `GET /activities/{activityId}/lobby`
- `POST /activities/{activityId}/ping`
- `POST /activities/{activityId}/leave`
- `POST /activities/{activityId}/reserve`
- `DELETE /activities/{activityId}/reserve`
- `POST /activities/{activityId}/confirm`
- `GET /activities/{activityId}/messages`
- `POST /activities/{activityId}/messages`

其中 `GET /activities/{activityId}/lobby` 是核心接口，要求一次性返回大厅可视化所需数据。

建议返回结构：

```json
{
  "activityId": 1,
  "title": "俱乐部 A 今晚活动",
  "venueName": "晴天羽毛球馆",
  "clubName": "俱乐部 A",
  "timeSlot": "EVENING",
  "status": "open",
  "onlineCount": 12,
  "broadcast": {
    "content": "1号场高质量缺人，L4以上速来"
  },
  "courts": [
    {
      "courtNumber": 1,
      "status": "recruiting",
      "confirmedCount": 3,
      "slots": [
        {
          "slotNumber": 1,
          "userId": 1001,
          "nickname": "张三",
          "avatar": "/avatars/a.png",
          "level": "L4",
          "status": "confirmed"
        }
      ]
    }
  ],
  "observers": [
    {
      "userId": 1002,
      "nickname": "李四",
      "avatar": "/avatars/b.png",
      "level": "L3"
    }
  ],
  "messages": [
    {
      "id": 10,
      "nickname": "张三",
      "level": "L4",
      "content": "1号场差一个人，来不？",
      "createdAt": "2026-05-12T19:40:00"
    }
  ]
}
```

### 6.4 BroadcastController

- `GET /broadcasts/active`
- `POST /clubs/{clubId}/broadcasts`

规则：

1. 普通用户不能调用广播发送接口
2. 只有俱乐部具备广播权限
3. 当前阶段不做次数购买，但表字段要预留

---

## 七、初始化与定时任务

### 7.1 初始化数据

服务启动后或通过 SQL 脚本，初始化以下数据：

#### 球馆

- 晴天羽毛球馆

#### 俱乐部

- 俱乐部 A
- 俱乐部 B
- 俱乐部 C

#### 活动

建议每个俱乐部初始化 1 个今晚活动，便于客户端联调。

### 7.2 定时任务

至少需要 3 类清理任务：

1. 清理过期广播
2. 清理前一天聊天消息
3. 清理超时 presence 记录，并按规则释放超时坑位

当前阶段直接使用 Spring `@Scheduled` 即可。

---

## 八、推荐开发顺序

1. 建立 migration 目录并补 V3 脚本
2. 创建 Entity / Mapper / DTO
3. 完成初始化数据能力
4. 完成球馆列表与俱乐部列表接口
5. 完成大厅快照接口
6. 完成占坑 / 确认 / 释放逻辑
7. 完成聊天接口
8. 完成广播接口
9. 完成定时任务

---

## 九、验收标准

1. 服务端能初始化晴天羽毛球馆和 3 个俱乐部
2. `GET /venues` 可返回球馆列表
3. `GET /venues/{venueId}/clubs` 可返回俱乐部列表
4. `GET /activities/{activityId}/lobby` 返回结构稳定且足够支撑客户端渲染
5. 同一用户不能占多个坑
6. 6 人全部确认后，场地进入 locked
7. 聊天 60 秒限频生效
8. 广播能查询、能过期
9. 在线状态和超时清理逻辑正常

---

## 十、结论

服务端的核心不是把页面字段拼出来，而是保证这几个方面稳定：

1. 数据结构清晰
2. 状态流转正确
3. 大厅快照接口足够完整
4. 占坑、确认、成局规则一致
5. 后续可扩展到更多球馆、更多俱乐部、更多时间段