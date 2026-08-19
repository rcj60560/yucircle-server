package com.yucircle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yucircle.dto.ClubActivityDto;
import com.yucircle.dto.ClubDto;
import com.yucircle.dto.CreateClubRequest;
import com.yucircle.entity.Club;

import java.util.List;

public interface ClubService extends IService<Club> {
    List<ClubDto> listClubsByVenue(Long venueId);

    List<ClubActivityDto> listClubActivities(Long clubId);

    Club createClub(Long userId, CreateClubRequest request);
}
