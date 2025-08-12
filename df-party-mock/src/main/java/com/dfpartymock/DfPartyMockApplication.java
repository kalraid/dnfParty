package com.dfpartymock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DfPartyMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DfPartyMockApplication.class, args);
    }
}
