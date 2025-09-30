package com.example.learnverse;

import com.example.learnverse.auth.admin.AdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AdminProperties.class)
public class LearnVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnVerseApplication.class, args);
    }

}
