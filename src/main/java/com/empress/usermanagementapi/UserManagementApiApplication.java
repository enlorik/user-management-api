package com.empress.usermanagementapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApiApplication.class, args);
    }

}
