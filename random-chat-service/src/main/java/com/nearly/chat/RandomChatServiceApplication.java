package com.nearly.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class RandomChatServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RandomChatServiceApplication.class, args);
    }
}

