package com.yucircle.dto;

import lombok.Data;

@Data
public class VenueDto {
    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer courtCount;
    private Double latitude;
    private Double longitude;
    private Double distance;
    private String broadcastMessage;
    private String distanceText;
    private Integer onlineCount;
    private Boolean hasActiveBroadcast;
}
