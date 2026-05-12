package com.yucircle.dto;

import lombok.Data;

@Data
public class CreateClubRequest {
    private Long venueId;
    private String name;
    private String description;
}
