package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UploaderServiceConfig.class)
public class UploaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(UploaderApplication.class, args);
    }
}
