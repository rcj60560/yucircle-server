package com.yucircle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.VenueDto;
import com.yucircle.entity.Venue;
import com.yucircle.mapper.VenueMapper;
import com.yucircle.service.VenueService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

    @Override
    public List<VenueDto> listVenues() {
        List<Venue> venues = list();
        return venues.stream().map(v -> {
            VenueDto dto = new VenueDto();
            dto.setId(v.getId());
            dto.setName(v.getName());
            dto.setAddress(v.getAddress());
            dto.setCourtCount(v.getCourtCount());
            dto.setDistanceText("未知距离");
            dto.setOnlineCount(0);
            dto.setHasActiveBroadcast(false);
            return dto;
        }).collect(Collectors.toList());
    }
}
