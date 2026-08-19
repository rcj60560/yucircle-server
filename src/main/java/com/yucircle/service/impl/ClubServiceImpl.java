package com.yucircle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.ClubActivityDto;
import com.yucircle.dto.ClubDto;
import com.yucircle.dto.CreateClubRequest;
import com.yucircle.entity.*;
import com.yucircle.mapper.*;
import com.yucircle.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClubServiceImpl extends ServiceImpl<ClubMapper, Club> implements ClubService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivitySlotMapper activitySlotMapper;

    @Autowired
    private ActivityPresenceMapper activityPresenceMapper;

    @Autowired
    private BroadcastMapper broadcastMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<ClubDto> listClubsByVenue(Long venueId) {
        LambdaQueryWrapper<Club> query = new LambdaQueryWrapper<>();
        query.eq(Club::getVenueId, venueId)
             .eq(Club::getStatus, "active")
             .orderByDesc(Club::getCreatedAt);
        List<Club> clubs = list(query);

        if (clubs.isEmpty()) {
            clubs = createDefaultClubs(venueId);
        }

        LocalDate today = LocalDate.now();

        return clubs.stream().limit(3).map(c -> {
            ClubDto dto = new ClubDto();
            dto.setId(c.getId());
            dto.setVenueId(c.getVenueId());
            dto.setName(c.getName());
            dto.setDescription(c.getDescription());
            dto.setCreatorId(c.getCreatorId());
            dto.setBroadcastCredits(c.getBroadcastCredits());
            dto.setCreatedAt(c.getCreatedAt() != null
                ? c.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Activity nextActivity = ensureAvailableActivity(c);
            dto.setNextActivityId(nextActivity.getId());
            dto.setNextActivityTitle(nextActivity.getTitle());
            dto.setNextActivityTime(nextActivity.getActivityDate() + " " + nextActivity.getTimeSlot());
            dto.setNextActivityDescription(buildActivityDescription(nextActivity));

            LambdaQueryWrapper<ActivityPresence> activityPresenceQuery = new LambdaQueryWrapper<>();
            activityPresenceQuery.eq(ActivityPresence::getActivityId, nextActivity.getId());
            dto.setNextActivityOnlineCount(activityPresenceMapper.selectCount(activityPresenceQuery).intValue());

            User organizer = userMapper.selectById(nextActivity.getCreatorId());
            dto.setOrganizerName(organizer != null && organizer.getNickname() != null
                ? organizer.getNickname()
                : "组织者");

            // Count activities for today
            LambdaQueryWrapper<Activity> activityQuery = new LambdaQueryWrapper<>();
            activityQuery.eq(Activity::getClubId, c.getId())
                        .eq(Activity::getActivityDate, today);
            long todayActivityCount = activityMapper.selectCount(activityQuery);
            dto.setTodayActivityCount((int) todayActivityCount);

            // Count online users (active presence records from today's activities)
            int onlineCount = 0;
            List<Activity> todayActivities = activityMapper.selectList(activityQuery);
            for (Activity activity : todayActivities) {
                LambdaQueryWrapper<ActivityPresence> presenceQuery = new LambdaQueryWrapper<>();
                presenceQuery.eq(ActivityPresence::getActivityId, activity.getId());
                long presenceCount = activityPresenceMapper.selectCount(presenceQuery);
                onlineCount += presenceCount;
            }
            dto.setOnlineCount(onlineCount);

            // Check if club has active broadcast
            LambdaQueryWrapper<Broadcast> broadcastQuery = new LambdaQueryWrapper<>();
            broadcastQuery.eq(Broadcast::getClubId, c.getId())
                         .gt(Broadcast::getExpiresAt, LocalDateTime.now());
            long broadcastCount = broadcastMapper.selectCount(broadcastQuery);
            dto.setHasActiveBoradcast(broadcastCount > 0);

            dto.setStatus(c.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ClubActivityDto> listClubActivities(Long clubId) {
        LambdaQueryWrapper<Activity> query = new LambdaQueryWrapper<>();
        query.eq(Activity::getClubId, clubId)
             .ge(Activity::getActivityDate, LocalDate.now())
             .orderByAsc(Activity::getActivityDate)
             .orderByAsc(Activity::getTimeSlot);

        List<Activity> activities = activityMapper.selectList(query);
        if (activities.isEmpty()) {
            Club club = getById(clubId);
            if (club != null) {
                activities = List.of(ensureAvailableActivity(club));
            }
        }

        return activities.stream().map(a -> {
            ClubActivityDto dto = new ClubActivityDto();
            dto.setId(a.getId());
            dto.setTitle(a.getTitle());
            dto.setActivityDate(a.getActivityDate() != null ? a.getActivityDate().toString() : "");
            dto.setTimeSlot(a.getTimeSlot());
            dto.setDescription(buildActivityDescription(a));
            User organizer = userMapper.selectById(a.getCreatorId());
            dto.setOrganizerName(organizer != null && organizer.getNickname() != null
                ? organizer.getNickname()
                : "组织者");
            LambdaQueryWrapper<ActivityPresence> presenceQuery = new LambdaQueryWrapper<>();
            presenceQuery.eq(ActivityPresence::getActivityId, a.getId());
            dto.setOnlineCount(activityPresenceMapper.selectCount(presenceQuery).intValue());
            dto.setStatus(a.getStatus());
            dto.setCourtCount(a.getCourtCount());
            dto.setMaxPerCourt(a.getMaxPerCourt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Club createClub(Long userId, CreateClubRequest request) {
        Club club = new Club();
        club.setVenueId(request.getVenueId());
        club.setName(request.getName());
        club.setDescription(request.getDescription());
        club.setCreatorId(userId);
        club.setStatus("active");
        club.setBroadcastCredits(0);
        club.setCreatedAt(LocalDateTime.now());
        club.setUpdatedAt(LocalDateTime.now());
        save(club);
        return club;
    }

    private List<Club> createDefaultClubs(Long venueId) {
        Long creatorId = 1L;
        User firstUser = userMapper.selectById(1L);
        if (firstUser == null) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.last("LIMIT 1");
            firstUser = userMapper.selectOne(userQuery);
            if (firstUser != null) {
                creatorId = firstUser.getId();
            }
        }

        Club clubA = createDefaultClub(venueId, creatorId, "俱乐部 A", "以双打为主，欢迎稳定出勤球友");
        Club clubB = createDefaultClub(venueId, creatorId, "俱乐部 B", "快节奏对抗，偏进攻打法");
        Club clubC = createDefaultClub(venueId, creatorId, "俱乐部 C", "新手友好，注重体验与陪练");
        return List.of(clubA, clubB, clubC);
    }

    private Club createDefaultClub(Long venueId, Long creatorId, String name, String description) {
        Club club = new Club();
        club.setVenueId(venueId);
        club.setName(name);
        club.setDescription(description);
        club.setCreatorId(creatorId);
        club.setStatus("active");
        club.setBroadcastCredits(0);
        club.setCreatedAt(LocalDateTime.now());
        club.setUpdatedAt(LocalDateTime.now());
        save(club);
        return club;
    }

    private Activity ensureAvailableActivity(Club club) {
        LambdaQueryWrapper<Activity> query = new LambdaQueryWrapper<>();
        query.eq(Activity::getClubId, club.getId())
             .ge(Activity::getActivityDate, LocalDate.now())
             .orderByAsc(Activity::getActivityDate)
             .orderByAsc(Activity::getTimeSlot)
             .last("LIMIT 1");
        Activity activity = activityMapper.selectOne(query);
        if (activity != null) {
            return activity;
        }

        Activity demo = new Activity();
        demo.setVenueId(club.getVenueId());
        demo.setClubId(club.getId());
        demo.setCreatorId(club.getCreatorId());
        demo.setTitle(club.getName() + " - 今晚活动");
        demo.setActivityDate(LocalDate.now());
        demo.setTimeSlot("20:00-22:00");
        demo.setLevelRequirement("L1-L8");
        demo.setMatchType("双打");
        demo.setCourtCount(4);  // Phase4_Demo: 改为 4 场地
        demo.setMaxPerCourt(6);
        demo.setStatus("open");
        demo.setCreatedAt(LocalDateTime.now());
        demo.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(demo);

        // 创建 4 个场地的 36 个坑位，并预填充模拟数据
        int slotIndexGlobal = 0;
        for (int courtNum = 1; courtNum <= demo.getCourtCount(); courtNum++) {
            for (int slotNum = 1; slotNum <= demo.getMaxPerCourt(); slotNum++) {
                ActivitySlot slot = new ActivitySlot();
                slot.setActivityId(demo.getId());
                slot.setCourtNumber(courtNum);
                slot.setSlotNumber(slotNum);
                slot.setCreatedAt(LocalDateTime.now());
                slot.setUpdatedAt(LocalDateTime.now());

                // Phase4_Demo: 预填充模拟数据以营造热闹氛围
                // 场 1: 3/6 已确认 + 1 个已占坑
                if (courtNum == 1) {
                    if (slotNum <= 3) {
                        slot.setSlotStatus("confirmed");
                        slot.setUserId((long)(1000 + slotIndexGlobal));
                    } else if (slotNum == 4) {
                        slot.setSlotStatus("reserved");
                        slot.setUserId((long)(1000 + slotIndexGlobal));
                    } else {
                        slot.setSlotStatus("empty");
                    }
                }
                // 场 2: 1/6 已确认
                else if (courtNum == 2) {
                    if (slotNum == 1) {
                        slot.setSlotStatus("confirmed");
                        slot.setUserId((long)(1000 + slotIndexGlobal));
                    } else {
                        slot.setSlotStatus("empty");
                    }
                }
                // 场 3: 6/6 已确认（满局）
                else if (courtNum == 3) {
                    slot.setSlotStatus("confirmed");
                    slot.setUserId((long)(1000 + slotIndexGlobal));
                }
                // 场 4: 全部空坑
                else {
                    slot.setSlotStatus("empty");
                }

                activitySlotMapper.insert(slot);
                slotIndexGlobal++;
            }
        }

        // 创建模拟用户（使用现有的用户或直接用 ID）
        // 同时创建 ActivityPresence 记录来模拟在线人数
        int totalFakeUsers = 25;
        for (int i = 0; i < totalFakeUsers; i++) {
            ActivityPresence presence = new ActivityPresence();
            presence.setActivityId(demo.getId());
            presence.setUserId((long)(2000 + i)); // 模拟用户 ID
            presence.setLastPingAt(LocalDateTime.now().minusSeconds(5 + i)); // 分散时间避免重复
            presence.setCreatedAt(LocalDateTime.now());
            activityPresenceMapper.insert(presence);
        }

        return demo;
    }

    private String buildActivityDescription(Activity activity) {
        return "时间" + activity.getTimeSlot() + "，" + activity.getMatchType() + "，等级 " +
            (activity.getLevelRequirement() != null ? activity.getLevelRequirement() : "L1-L8");
    }
}
