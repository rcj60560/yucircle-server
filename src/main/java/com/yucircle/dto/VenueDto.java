package com.yucircle.dto;

import lombok.Data;

@Data
public class VenueDto {
    private Long id;
    private String name;
    private String address;
    private Integer courtCount;
    private String distanceText;
    private Integer onlineCount;
    private Boolean hasActiveBroadcast;
}
