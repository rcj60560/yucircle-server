package com.yucircle.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yucircle.dto.VenueDto;
import com.yucircle.entity.Venue;
import com.yucircle.mapper.VenueMapper;
import com.yucircle.service.VenueService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

    @Override
    public List<VenueDto> listVenues() {
        // 暂时固定返回一个场馆，后续可切换为注册球馆动态列表。
        VenueDto dto = new VenueDto();
        dto.setId(1L);
        dto.setName("晴天");
        dto.setCity("成都");
        dto.setAddress("成都市武侯区三利运动中心");
        dto.setCourtCount(10);
        dto.setLatitude(30.5528);
        dto.setLongitude(104.0666);
        dto.setDistance(2.0);
        dto.setBroadcastMessage("欢迎进入羽圈活动大厅");
        dto.setDistanceText("2.0km");
        dto.setOnlineCount(0);
        dto.setHasActiveBroadcast(false);
        return Collections.singletonList(dto);
    }
}
