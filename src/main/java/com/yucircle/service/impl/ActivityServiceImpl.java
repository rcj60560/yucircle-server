package com.yucircle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.*;
import com.yucircle.entity.*;
import com.yucircle.mapper.*;
import com.yucircle.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Autowired
    private ActivitySlotMapper activitySlotMapper;

    @Autowired
    private ActivityPresenceMapper activityPresenceMapper;

    @Autowired
    private ActivityMessageMapper activityMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private BroadcastMapper broadcastMapper;

    private static final int SLOT_LOCK_TIMEOUT_SECONDS = 180; // 3 minutes
    private static final int PING_TIMEOUT_SECONDS = 30;

    @Override
    public Activity createActivity(Long userId, CreateActivityRequest request) {
        Activity activity = new Activity();
        activity.setVenueId(request.getVenueId());
        activity.setClubId(request.getClubId());
        activity.setCreatorId(userId);
        activity.setTitle(request.getTitle());
        activity.setActivityDate(LocalDate.parse(request.getActivityDate()));
        activity.setTimeSlot(request.getTimeSlot());
        activity.setMatchType(request.getMatchType());
        activity.setCourtCount(request.getCourtCount());
        activity.setMaxPerCourt(request.getMaxPerCourt());
        activity.setStatus("open");
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        save(activity);

        // Initialize slots for all courts
        initializeSlots(activity);
        return activity;
    }

    private void initializeSlots(Activity activity) {
        for (int courtNum = 1; courtNum <= activity.getCourtCount(); courtNum++) {
            for (int slotNum = 1; slotNum <= activity.getMaxPerCourt(); slotNum++) {
                ActivitySlot slot = new ActivitySlot();
                slot.setActivityId(activity.getId());
                slot.setCourtNumber(courtNum);
                slot.setSlotNumber(slotNum);
                slot.setSlotStatus("empty");
                slot.setCreatedAt(LocalDateTime.now());
                slot.setUpdatedAt(LocalDateTime.now());
                activitySlotMapper.insert(slot);
            }
        }
    }

    @Override
    public Activity enterLobby(Long userId, Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new RuntimeException("Activity not found");
        }

        // Insert or update presence
        LambdaQueryWrapper<ActivityPresence> query = new LambdaQueryWrapper<>();
        query.eq(ActivityPresence::getActivityId, activityId)
             .eq(ActivityPresence::getUserId, userId);
        ActivityPresence presence = activityPresenceMapper.selectOne(query);

        if (presence == null) {
            presence = new ActivityPresence();
            presence.setActivityId(activityId);
            presence.setUserId(userId);
            presence.setLastPingAt(LocalDateTime.now());
            presence.setCreatedAt(LocalDateTime.now());
            activityPresenceMapper.insert(presence);
        } else {
            presence.setLastPingAt(LocalDateTime.now());
            activityPresenceMapper.updateById(presence);
        }

        return activity;
    }

    @Override
    public ActivityLobbyDto getLobbySnapshot(Long activityId) {
        return getLobbySnapshot(activityId, null);
    }

    @Override
    public ActivityLobbyDto getLobbySnapshot(Long activityId, Long userId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new RuntimeException("Activity not found");
        }

        Venue venue = venueMapper.selectById(activity.getVenueId());
        Club club = activity.getClubId() != null ? clubMapper.selectById(activity.getClubId()) : null;

        ActivityLobbyDto lobby = new ActivityLobbyDto();
        lobby.setActivityId(activityId);
        lobby.setTitle(activity.getTitle());
        lobby.setVenueName(venue != null ? venue.getName() : "");
        lobby.setClubName(club != null ? club.getName() : "");
        lobby.setTimeSlot(activity.getTimeSlot());
        lobby.setMatchType(activity.getMatchType());
        lobby.setLevelRequirement(activity.getLevelRequirement());
        lobby.setStatus(activity.getStatus());

        // Count online users
        LambdaQueryWrapper<ActivityPresence> presenceQuery = new LambdaQueryWrapper<>();
        presenceQuery.eq(ActivityPresence::getActivityId, activityId);
        long onlineCount = activityPresenceMapper.selectCount(presenceQuery);
        lobby.setOnlineCount((int) onlineCount);

        // Count online users at venue level (deduplicated users in same venue, recent ping only)
        LambdaQueryWrapper<Activity> venueActivityQuery = new LambdaQueryWrapper<>();
        venueActivityQuery.eq(Activity::getVenueId, activity.getVenueId());
        List<Activity> venueActivities = list(venueActivityQuery);

        int venueOnlineCount = 0;
        if (!venueActivities.isEmpty()) {
            List<Long> venueActivityIds = venueActivities.stream()
                .map(Activity::getId)
                .collect(Collectors.toList());
            LocalDateTime activeThreshold = LocalDateTime.now().minusSeconds(PING_TIMEOUT_SECONDS);

            LambdaQueryWrapper<ActivityPresence> venuePresenceQuery = new LambdaQueryWrapper<>();
            venuePresenceQuery.in(ActivityPresence::getActivityId, venueActivityIds)
                .ge(ActivityPresence::getLastPingAt, activeThreshold);
            List<ActivityPresence> venuePresences = activityPresenceMapper.selectList(venuePresenceQuery);

            Set<Long> venueOnlineUserIds = venuePresences.stream()
                .map(ActivityPresence::getUserId)
                .collect(Collectors.toSet());
            venueOnlineCount = venueOnlineUserIds.size();
        }
        lobby.setVenueOnlineCount(venueOnlineCount);

        // Get active broadcast
        LambdaQueryWrapper<Broadcast> broadcastQuery = new LambdaQueryWrapper<>();
        broadcastQuery.eq(Broadcast::getClubId, activity.getClubId())
                     .gt(Broadcast::getExpiresAt, LocalDateTime.now())
                     .orderByDesc(Broadcast::getCreatedAt)
                     .last("LIMIT 1");
        Broadcast broadcast = broadcastMapper.selectOne(broadcastQuery);
        if (broadcast != null) {
            BroadcastDto broadcastDto = new BroadcastDto();
            broadcastDto.setId(broadcast.getId());
            broadcastDto.setContent(broadcast.getContent());
            broadcastDto.setExpiresAt(broadcast.getExpiresAt().toString());
            lobby.setBroadcast(broadcastDto);
        }

        // Get courts
        // Get all slots for this activity first
        LambdaQueryWrapper<ActivitySlot> allSlotsQuery = new LambdaQueryWrapper<>();
        allSlotsQuery.eq(ActivitySlot::getActivityId, activityId)
                     .orderByAsc(ActivitySlot::getCourtNumber)
                     .orderByAsc(ActivitySlot::getSlotNumber);
        List<ActivitySlot> allSlots = activitySlotMapper.selectList(allSlotsQuery);

        List<CourtDto> courts = new ArrayList<>();
        for (int courtNum = 1; courtNum <= activity.getCourtCount(); courtNum++) {
            final int currentCourtNum = courtNum; // Make final for lambda
            CourtDto courtDto = new CourtDto();
            courtDto.setCourtNumber(courtNum);

            // Get slots for this court
            List<ActivitySlot> courtSlots = allSlots.stream()
                .filter(s -> s.getCourtNumber().equals(currentCourtNum))
                .collect(Collectors.toList());

            List<SlotDto> slotDtos = new ArrayList<>();
            int confirmedCount = 0;
            for (ActivitySlot slot : courtSlots) {
                SlotDto slotDto = new SlotDto();
                slotDto.setSlotNumber(slot.getSlotNumber());
                slotDto.setStatus(slot.getSlotStatus());
                slotDto.setIsMine(userId != null && userId.equals(slot.getUserId()));

                if (slot.getUserId() != null) {
                    User user = userMapper.selectById(slot.getUserId());
                    if (user != null) {
                        slotDto.setUserId(slot.getUserId());
                        slotDto.setNickname(user.getNickname());
                        slotDto.setAvatar(user.getAvatar());
                        slotDto.setLevel(user.getBadmintonLevel());
                        if ("confirmed".equals(slot.getSlotStatus())) {
                            confirmedCount++;
                        }
                    }
                }
                slotDtos.add(slotDto);
            }

            courtDto.setSlots(slotDtos);
            courtDto.setConfirmedCount(confirmedCount);

            // Determine court status
            if (confirmedCount >= activity.getMaxPerCourt()) {
                courtDto.setStatus("locked");
            } else {
                courtDto.setStatus("recruiting");
            }

            courts.add(courtDto);
        }
        lobby.setCourts(courts);

        // Get observers (users without reserved slots)
        Set<Long> userIdsWithSlots = allSlots.stream()
            .filter(s -> s.getUserId() != null)
            .map(ActivitySlot::getUserId)
            .collect(Collectors.toSet());

        LambdaQueryWrapper<ActivityPresence> observerQuery = new LambdaQueryWrapper<>();
        observerQuery.eq(ActivityPresence::getActivityId, activityId);
        List<ActivityPresence> presences = activityPresenceMapper.selectList(observerQuery);

        List<ObserverDto> observers = new ArrayList<>();
        for (ActivityPresence presence : presences) {
            if (!userIdsWithSlots.contains(presence.getUserId())) {
                User user = userMapper.selectById(presence.getUserId());
                if (user != null) {
                    ObserverDto observerDto = new ObserverDto();
                    observerDto.setUserId(presence.getUserId());
                    observerDto.setNickname(user.getNickname());
                    observerDto.setAvatar(user.getAvatar());
                    observerDto.setLevel(user.getBadmintonLevel());
                    observers.add(observerDto);
                }
            }
        }
        lobby.setObservers(observers);

        // Get recent messages
        LambdaQueryWrapper<ActivityMessage> messageQuery = new LambdaQueryWrapper<>();
        messageQuery.eq(ActivityMessage::getActivityId, activityId)
                   .orderByDesc(ActivityMessage::getCreatedAt)
                   .last("LIMIT 50");
        List<ActivityMessage> messages = activityMessageMapper.selectList(messageQuery);
        Collections.reverse(messages);

        List<ActivityMessageDto> messageDtos = new ArrayList<>();
        for (ActivityMessage msg : messages) {
            User user = userMapper.selectById(msg.getUserId());
            if (user != null) {
                ActivityMessageDto msgDto = new ActivityMessageDto();
                msgDto.setId(msg.getId());
                msgDto.setNickname(user.getNickname());
                msgDto.setLevel(user.getBadmintonLevel());
                msgDto.setContent(msg.getContent());
                msgDto.setCreatedAt(msg.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_TIME));
                messageDtos.add(msgDto);
            }
        }
        lobby.setMessages(messageDtos);


        return lobby;
    }

    @Override
    public ActivitySlotDto reserveSlot(Long userId, Long activityId, ReserveSlotRequest request) {
        // Check if user already has a slot in this activity
        LambdaQueryWrapper<ActivitySlot> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.eq(ActivitySlot::getActivityId, activityId)
                     .eq(ActivitySlot::getUserId, userId);
        ActivitySlot existingSlot = activitySlotMapper.selectOne(existingQuery);
        if (existingSlot != null) {
            throw new RuntimeException("User already has a slot in this activity");
        }

        // Find and lock the slot
        LambdaQueryWrapper<ActivitySlot> slotQuery = new LambdaQueryWrapper<>();
        slotQuery.eq(ActivitySlot::getActivityId, activityId)
                 .eq(ActivitySlot::getCourtNumber, request.getCourtNumber())
                 .eq(ActivitySlot::getSlotNumber, request.getSlotNumber());
        ActivitySlot slot = activitySlotMapper.selectOne(slotQuery);

        if (slot == null || !"empty".equals(slot.getSlotStatus())) {
            throw new RuntimeException("Slot is not available");
        }

        // Reserve the slot
        slot.setUserId(userId);
        slot.setSlotStatus("reserved");
        slot.setLockExpiresAt(LocalDateTime.now().plusSeconds(SLOT_LOCK_TIMEOUT_SECONDS));
        slot.setUpdatedAt(LocalDateTime.now());
        activitySlotMapper.updateById(slot);

        return buildSlotDto(slot, userId);
    }

    @Override
    public void cancelSlot(Long userId, Long activityId) {
        LambdaQueryWrapper<ActivitySlot> query = new LambdaQueryWrapper<>();
        query.eq(ActivitySlot::getActivityId, activityId)
             .eq(ActivitySlot::getUserId, userId);
        ActivitySlot slot = activitySlotMapper.selectOne(query);

        if (slot != null) {
            slot.setUserId(null);
            slot.setSlotStatus("empty");
            slot.setLockExpiresAt(null);
            slot.setUpdatedAt(LocalDateTime.now());
            activitySlotMapper.updateById(slot);
        }
    }

    @Override
    public ActivitySlotDto confirmSlot(Long userId, Long activityId) {
        LambdaQueryWrapper<ActivitySlot> query = new LambdaQueryWrapper<>();
        query.eq(ActivitySlot::getActivityId, activityId)
             .eq(ActivitySlot::getUserId, userId);
        ActivitySlot slot = activitySlotMapper.selectOne(query);

        if (slot == null) {
            throw new RuntimeException("User has no slot in this activity");
        }

        slot.setSlotStatus("confirmed");
        slot.setLockExpiresAt(null);
        slot.setUpdatedAt(LocalDateTime.now());
        activitySlotMapper.updateById(slot);

        // Check if all slots in this court are confirmed
        Activity activity = getById(activityId);
        LambdaQueryWrapper<ActivitySlot> courtSlotsQuery = new LambdaQueryWrapper<>();
        courtSlotsQuery.eq(ActivitySlot::getActivityId, activityId)
                       .eq(ActivitySlot::getCourtNumber, slot.getCourtNumber());
        List<ActivitySlot> courtSlots = activitySlotMapper.selectList(courtSlotsQuery);

        long confirmedCount = courtSlots.stream()
            .filter(s -> "confirmed".equals(s.getSlotStatus()))
            .count();

        if (confirmedCount >= activity.getMaxPerCourt()) {
            // Mark all slots in this court as locked
            for (ActivitySlot s : courtSlots) {
                if (!"locked".equals(s.getSlotStatus())) {
                    s.setSlotStatus("locked");
                    s.setUpdatedAt(LocalDateTime.now());
                    activitySlotMapper.updateById(s);
                }
            }
        }

        return buildSlotDto(slot, userId);
    }

    @Override
    public ActivityMessageDto sendMessage(Long userId, Long activityId, SendActivityMessageRequest request) {
        // Check rate limit: 60 seconds between messages
        LambdaQueryWrapper<ActivityMessage> recentQuery = new LambdaQueryWrapper<>();
        recentQuery.eq(ActivityMessage::getActivityId, activityId)
                   .eq(ActivityMessage::getUserId, userId)
                   .gt(ActivityMessage::getCreatedAt, LocalDateTime.now().minusSeconds(60))
                   .orderByDesc(ActivityMessage::getCreatedAt)
                   .last("LIMIT 1");
        ActivityMessage lastMessage = activityMessageMapper.selectOne(recentQuery);

        if (lastMessage != null) {
            throw new RuntimeException("Message rate limit: 1 message per 60 seconds");
        }

        ActivityMessage message = new ActivityMessage();
        message.setActivityId(activityId);
        message.setUserId(userId);
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());
        activityMessageMapper.insert(message);

        User user = userMapper.selectById(userId);
        ActivityMessageDto dto = new ActivityMessageDto();
        dto.setId(message.getId());
        dto.setNickname(user != null ? user.getNickname() : "Anonymous");
        dto.setLevel(user != null ? user.getBadmintonLevel() : "");
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_TIME));

        return dto;
    }

    @Override
    public void ping(Long userId, Long activityId) {
        LambdaQueryWrapper<ActivityPresence> query = new LambdaQueryWrapper<>();
        query.eq(ActivityPresence::getActivityId, activityId)
             .eq(ActivityPresence::getUserId, userId);
        ActivityPresence presence = activityPresenceMapper.selectOne(query);

        if (presence != null) {
            presence.setLastPingAt(LocalDateTime.now());
            activityPresenceMapper.updateById(presence);
        }
    }

    @Override
    public void leaveLobby(Long userId, Long activityId) {
        // Cancel slot if user has one reserved
        cancelSlot(userId, activityId);
        // Delete presence record
        LambdaQueryWrapper<ActivityPresence> query = new LambdaQueryWrapper<>();
        query.eq(ActivityPresence::getActivityId, activityId)
             .eq(ActivityPresence::getUserId, userId);
        activityPresenceMapper.delete(query);
    }

    @Override
    public void cleanupExpiredPresence() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(PING_TIMEOUT_SECONDS);
        LambdaQueryWrapper<ActivityPresence> query = new LambdaQueryWrapper<>();
        query.lt(ActivityPresence::getLastPingAt, threshold);
        activityPresenceMapper.delete(query);
    }

    @Override
    public void cleanupExpiredSlots() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ActivitySlot> query = new LambdaQueryWrapper<>();
        query.eq(ActivitySlot::getSlotStatus, "reserved")
             .le(ActivitySlot::getLockExpiresAt, now);
        List<ActivitySlot> expiredSlots = activitySlotMapper.selectList(query);

        for (ActivitySlot slot : expiredSlots) {
            slot.setUserId(null);
            slot.setSlotStatus("empty");
            slot.setLockExpiresAt(null);
            slot.setUpdatedAt(LocalDateTime.now());
            activitySlotMapper.updateById(slot);
        }
    }

    @Override
    public void cleanupOldMessages() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LambdaQueryWrapper<ActivityMessage> query = new LambdaQueryWrapper<>();
        query.lt(ActivityMessage::getCreatedAt, yesterday);
        activityMessageMapper.delete(query);
    }

    private ActivitySlotDto buildSlotDto(ActivitySlot slot, Long userId) {
        User user = userMapper.selectById(userId);
        ActivitySlotDto dto = new ActivitySlotDto();
        dto.setSlotId(slot.getId());
        dto.setCourtNumber(slot.getCourtNumber());
        dto.setSlotNumber(slot.getSlotNumber());
        dto.setUserId(slot.getUserId());
        dto.setNickname(user != null ? user.getNickname() : "");
        dto.setAvatar(user != null ? user.getAvatar() : "");
        dto.setLevel(user != null ? user.getBadmintonLevel() : "");
        dto.setStatus(slot.getSlotStatus());
        return dto;
    }
}
