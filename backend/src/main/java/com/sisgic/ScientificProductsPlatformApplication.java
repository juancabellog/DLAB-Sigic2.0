package com.sisgic;

import com.sisgic.config.GeminiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(GeminiProperties.class)
public class ScientificProductsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScientificProductsPlatformApplication.class, args);
    }

}
