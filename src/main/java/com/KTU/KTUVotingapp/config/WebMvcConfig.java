package com.KTU.KTUVotingapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.images.location:uploads}")
    private String uploadLocation;

    @Value("${app.images.url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = urlPrefix + "**";
        Path uploadDir = Paths.get(uploadLocation).toAbsolutePath().normalize();
        registry.addResourceHandler(pattern)
                .addResourceLocations(uploadDir.toUri().toString())
                .setCachePeriod(3600);
    }
}

