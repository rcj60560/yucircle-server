package com.yucircle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yucircle.mapper")
@EnableScheduling
public class YuCircleApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuCircleApplication.class, args);
    }
}
