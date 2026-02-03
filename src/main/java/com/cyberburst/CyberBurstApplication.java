package com.cyberburst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CyberBurstApplication {

    public static void main(String[] args) {
        SpringApplication.run(CyberBurstApplication.class, args);
    }

}
