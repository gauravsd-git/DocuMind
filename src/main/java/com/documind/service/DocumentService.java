package com.documind.service;

import com.documind.dto.DocumentUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentUploadResponse upload(
            MultipartFile file,
            Long userId
    );
}