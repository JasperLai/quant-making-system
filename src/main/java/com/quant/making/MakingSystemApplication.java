package com.quant.making;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 量化做市系统 - 启动类
 */
@SpringBootApplication
@EnableScheduling
public class MakingSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MakingSystemApplication.class, args);
    }
}
