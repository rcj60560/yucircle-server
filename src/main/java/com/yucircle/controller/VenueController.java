package com.yucircle.controller;

import com.yucircle.dto.ApiResponse;
import com.yucircle.dto.VenueDto;
import com.yucircle.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/venues")
public class VenueController {

    @Autowired
    private VenueService venueService;

    @GetMapping
    public ApiResponse<List<VenueDto>> listVenues() {
        List<VenueDto> venues = venueService.listVenues();
        return ApiResponse.success(venues);
    }
}
