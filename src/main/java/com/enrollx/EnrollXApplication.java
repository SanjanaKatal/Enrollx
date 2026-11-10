package com.enrollx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EnrollXApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollXApplication.class, args);
    }
}
