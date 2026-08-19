package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.ClubActivityDto;
import com.yucircle.dto.ClubDto;
import com.yucircle.dto.CreateClubRequest;
import com.yucircle.entity.Club;
import com.yucircle.service.ClubService;
import com.yucircle.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs")
public class ClubController {

    @Autowired
    private ClubService clubService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/venue/{venueId}")
    public ApiResponse<List<ClubDto>> listClubsByVenue(@PathVariable Long venueId) {
        List<ClubDto> clubs = clubService.listClubsByVenue(venueId);
        return ApiResponse.success(clubs);
    }

    @PostMapping
    public ApiResponse<Club> createClub(@RequestBody CreateClubRequest request,
                                        HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Club club = clubService.createClub(userId, request);
        return ApiResponse.success(club);
    }

    @GetMapping("/{clubId}")
    public ApiResponse<Club> getClub(@PathVariable Long clubId) {
        Club club = clubService.getById(clubId);
        if (club == null) {
            return ApiResponse.error(404, "Club not found");
        }
        return ApiResponse.success(club);
    }

    @GetMapping("/{clubId}/activities")
    public ApiResponse<List<ClubActivityDto>> listClubActivities(@PathVariable Long clubId) {
        List<ClubActivityDto> activities = clubService.listClubActivities(clubId);
        return ApiResponse.success(activities);
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证token");
        }
        return jwtUtils.getUserIdFromToken(authHeader.substring(7));
    }
}
