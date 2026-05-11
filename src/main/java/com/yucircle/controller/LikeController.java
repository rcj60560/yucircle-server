package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.LikeRequest;
import com.yucircle.dto.LikeStatsResponse;
import com.yucircle.service.LikeService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ApiResponse<String> toggleLike(@RequestBody LikeRequest request,
                                          HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String result = likeService.toggleLike(userId, request);
        return ApiResponse.success(result);
    }

    @GetMapping("/stats/{objectType}/{objectId}")
    public ApiResponse<LikeStatsResponse> getStats(@PathVariable String objectType,
                                                   @PathVariable Long objectId,
                                                   HttpServletRequest httpRequest) {
        Long userId = tryGetUserId(httpRequest);
        return ApiResponse.success(likeService.getStats(objectType, objectId, userId));
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }

    private Long tryGetUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return jwtUtils.getUserIdFromToken(authHeader.substring(7));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
