package com.comicsai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComicsAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicsAiApplication.class, args);
    }
}
