package com.yucircle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yucircle.mapper")
public class YuCircleApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuCircleApplication.class, args);
    }
}
