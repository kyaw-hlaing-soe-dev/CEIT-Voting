package com.KTU.KTUVotingapp.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    // Store the multipart file and return a public URL or path to be stored in DB
    String store(MultipartFile file) throws Exception;

    Resource loadAsResource(String filename) throws Exception;
}

