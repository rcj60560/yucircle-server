package com.yucircle.dto;

import lombok.Data;
import com.yucircle.entity.User;

@Data
public class LoginResponse {
    private String token;
    private User user;

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }
}
