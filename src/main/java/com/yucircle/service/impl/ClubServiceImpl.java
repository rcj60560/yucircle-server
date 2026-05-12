package com.yucircle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.ClubDto;
import com.yucircle.dto.CreateClubRequest;
import com.yucircle.entity.*;
import com.yucircle.mapper.*;
import com.yucircle.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClubServiceImpl extends ServiceImpl<ClubMapper, Club> implements ClubService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityPresenceMapper activityPresenceMapper;

    @Autowired
    private BroadcastMapper broadcastMapper;

    @Override
    public List<ClubDto> listClubsByVenue(Long venueId) {
        LambdaQueryWrapper<Club> query = new LambdaQueryWrapper<>();
        query.eq(Club::getVenueId, venueId)
             .eq(Club::getStatus, "active")
             .orderByDesc(Club::getCreatedAt);
        List<Club> clubs = list(query);

        LocalDate today = LocalDate.now();

        return clubs.stream().map(c -> {
            ClubDto dto = new ClubDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setDescription(c.getDescription());

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
            dto.setHasBroadcast(broadcastCount > 0);

            dto.setStatus(c.getStatus());
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
}
