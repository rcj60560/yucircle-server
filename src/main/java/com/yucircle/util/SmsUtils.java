package com.yucircle.util;

import org.springframework.stereotype.Component;

@Component
public class SmsUtils {

    private static final String MOCK_CODE = "123456";

    /**
     * 发送短信验证码（模拟模式，始终使用 123456）
     */
    public String sendCode(String phone) {
        // TODO: 接入真实短信服务（阿里云等）
        return MOCK_CODE;
    }
}
