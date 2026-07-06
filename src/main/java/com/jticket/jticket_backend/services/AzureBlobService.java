package com.jticket.jticket_backend.services;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface AzureBlobService {
    String uploadFile(MultipartFile file) throws IOException;
    String generatePresignedUrl(String blobUrl);
}
