package com.yucircle.dto;

import lombok.Data;

@Data
public class VerifySmsCodeRequest {
    private String phone;
    private String code;
}
