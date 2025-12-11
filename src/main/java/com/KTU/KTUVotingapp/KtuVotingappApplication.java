package com.KTU.KTUVotingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableCaching
public class KtuVotingappApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(KtuVotingappApplication.class, args);
    }

}
