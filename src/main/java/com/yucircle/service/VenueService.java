package com.yucircle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yucircle.dto.VenueDto;
import com.yucircle.entity.Venue;

import java.util.List;

public interface VenueService extends IService<Venue> {
    List<VenueDto> listVenues();
}
