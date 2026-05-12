package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.BroadcastDto;
import com.yucircle.dto.SendBroadcastRequest;
import com.yucircle.entity.Broadcast;
import com.yucircle.service.BroadcastService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/broadcasts")
public class BroadcastController {

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/active")
    public ApiResponse<List<BroadcastDto>> listActiveBroadcasts(@RequestParam Long clubId) {
        List<BroadcastDto> broadcasts = broadcastService.listActiveBroadcasts(clubId);
        return ApiResponse.success(broadcasts);
    }

    @PostMapping("/clubs/{clubId}")
    public ApiResponse<Broadcast> sendBroadcast(@PathVariable Long clubId,
                                                @RequestBody SendBroadcastRequest request,
                                                HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Broadcast broadcast = broadcastService.sendBroadcast(userId, clubId, request);
        return ApiResponse.success(broadcast);
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }
}
