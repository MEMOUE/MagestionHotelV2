package com.mghbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MghBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MghBackendApplication.class, args);
    }

}