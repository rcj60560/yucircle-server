package com.yucircle.controller;

import com.yucircle.dto.*;
import com.yucircle.entity.Activity;
import com.yucircle.service.ActivityService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<Activity> createActivity(@RequestBody CreateActivityRequest request,
                                                HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Activity activity = activityService.createActivity(userId, request);
        return ApiResponse.success(activity);
    }

    @PostMapping("/{activityId}/enter")
    public ApiResponse<Activity> enterLobby(@PathVariable Long activityId,
                                           HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Activity activity = activityService.enterLobby(userId, activityId);
        return ApiResponse.success(activity);
    }

    @GetMapping("/{activityId}/lobby")
    public ApiResponse<ActivityLobbyDto> getLobbySnapshot(@PathVariable Long activityId) {
        ActivityLobbyDto lobby = activityService.getLobbySnapshot(activityId);
        return ApiResponse.success(lobby);
    }

    @PostMapping("/{activityId}/ping")
    public ApiResponse<Void> ping(@PathVariable Long activityId,
                                  HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        activityService.ping(userId, activityId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{activityId}/leave")
    public ApiResponse<Void> leaveLobby(@PathVariable Long activityId,
                                        HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        activityService.leaveLobby(userId, activityId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{activityId}/reserve")
    public ApiResponse<ActivitySlotDto> reserveSlot(@PathVariable Long activityId,
                                                     @RequestBody ReserveSlotRequest request,
                                                     HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ActivitySlotDto slot = activityService.reserveSlot(userId, activityId, request);
        return ApiResponse.success(slot);
    }

    @DeleteMapping("/{activityId}/reserve")
    public ApiResponse<Void> cancelSlot(@PathVariable Long activityId,
                                        HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        activityService.cancelSlot(userId, activityId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{activityId}/confirm")
    public ApiResponse<ActivitySlotDto> confirmSlot(@PathVariable Long activityId,
                                                    HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ActivitySlotDto slot = activityService.confirmSlot(userId, activityId);
        return ApiResponse.success(slot);
    }

    @GetMapping("/{activityId}/messages")
    public ApiResponse<Void> getMessages(@PathVariable Long activityId) {
        // Messages are included in lobby snapshot
        return ApiResponse.success(null);
    }

    @PostMapping("/{activityId}/messages")
    public ApiResponse<ActivityMessageDto> sendMessage(@PathVariable Long activityId,
                                                       @RequestBody SendActivityMessageRequest request,
                                                       HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ActivityMessageDto message = activityService.sendMessage(userId, activityId, request);
        return ApiResponse.success(message);
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }
}
