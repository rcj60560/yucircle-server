package com.yucircle.dto;

import lombok.Data;

@Data
public class SetupProfileRequest {
    private String nickname;
    private String avatar;
    private String bio;
    private String badmintonLevel;
}
