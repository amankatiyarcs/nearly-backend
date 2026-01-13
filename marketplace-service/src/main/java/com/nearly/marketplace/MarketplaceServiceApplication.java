package com.nearly.marketplace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication @EnableDiscoveryClient
public class MarketplaceServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MarketplaceServiceApplication.class, args); }
}

