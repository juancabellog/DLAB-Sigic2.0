package com.sisgic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ScientificProductsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScientificProductsPlatformApplication.class, args);
    }

}
