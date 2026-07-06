package com.jticket.jticket_backend.services.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.jticket.jticket_backend.services.AzureBlobService;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AzureBlobServiceImpl implements AzureBlobService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void init() {
        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        if (!blobContainerClient.exists()) {
            blobContainerClient.create();
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String blobName = UUID.randomUUID().toString() + extension;
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        blobClient.upload(file.getInputStream(), file.getSize(), true);

        return blobClient.getBlobUrl();
    }

    @Override
    public String generatePresignedUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isEmpty()) {
            return null;
        }
        try {
            String blobName = blobUrl.substring(blobUrl.lastIndexOf("/") + 1);
            if (blobName.contains("?")) {
                blobName = blobName.substring(0, blobName.indexOf("?"));
            }
            
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            if (!blobClient.exists()) {
                return blobUrl;
            }
            
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(15), 
                permission
            );
            
            String sasToken = blobClient.generateSas(values);
            return blobClient.getBlobUrl() + "?" + sasToken;
        } catch (Exception e) {
            return blobUrl;
        }
    }
}
