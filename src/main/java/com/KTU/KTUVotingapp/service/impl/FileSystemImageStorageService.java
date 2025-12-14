package com.KTU.KTUVotingapp.service.impl;

import com.KTU.KTUVotingapp.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemImageStorageService implements ImageStorageService {

    private final Path rootLocation;
    private final String serveUrlPrefix;

    public FileSystemImageStorageService(@Value("${app.images.location:uploads}") String locations,
                                         @Value("${app.images.url-prefix:/uploads/}") String serveUrlPrefix) throws IOException {
        this.rootLocation = Paths.get(locations).toAbsolutePath().normalize();
        this.serveUrlPrefix = serveUrlPrefix.endsWith("/") ? serveUrlPrefix : serveUrlPrefix + "/";
        Files.createDirectories(this.rootLocation);
    }

    @Override
    public String store(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) ext = original.substring(idx);
        String filename = UUID.randomUUID().toString() + ext;
        Path target = this.rootLocation.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to store file", e);
        }
        // Return a web-accessible path (the controller or static resource mapping should serve files under serveUrlPrefix)
        return this.serveUrlPrefix + filename;
    }

    @Override
    public Resource loadAsResource(String filename) throws Exception {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Could not read file: " + filename, e);
        }
    }
}

