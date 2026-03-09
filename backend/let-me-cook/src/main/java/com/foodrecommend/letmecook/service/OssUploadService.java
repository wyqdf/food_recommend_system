package com.foodrecommend.letmecook.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class OssUploadService {

    @Autowired(required = false)
    private OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    private static final String IMAGE_FOLDER = "images/";

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        ensureOssEnabled();

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        String newFileName = generateFileName(extension);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(extension));
            metadata.setContentLength(file.getSize());

            String objectName = IMAGE_FOLDER + newFileName;
            ossClient.putObject(bucketName, objectName, file.getInputStream(), metadata);

            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (IOException e) {
            throw new RuntimeException("上传文件到OSS失败: " + e.getMessage(), e);
        }
    }

    public String uploadRecipeImage(MultipartFile file, Long recipeId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        ensureOssEnabled();

        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = recipeId + "." + extension;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(extension));
            metadata.setContentLength(file.getSize());

            String objectName = IMAGE_FOLDER + newFileName;
            ossClient.putObject(bucketName, objectName, file.getInputStream(), metadata);

            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (IOException e) {
            throw new RuntimeException("上传文件到OSS失败: " + e.getMessage(), e);
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        ensureOssEnabled();

        try {
            String objectName = extractObjectName(imageUrl);
            if (objectName != null) {
                ossClient.deleteObject(bucketName, objectName);
            }
        } catch (Exception e) {
            throw new RuntimeException("删除OSS文件失败: " + e.getMessage(), e);
        }
    }

    private String generateFileName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "jpg";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "jpg";
        }
        String ext = filename.substring(lastDot + 1).toLowerCase();
        return ext.isEmpty() ? "jpg" : ext;
    }

    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String extractObjectName(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains(bucketName)) {
            return null;
        }
        int index = imageUrl.indexOf(bucketName + "." + endpoint + "/");
        if (index == -1) {
            return null;
        }
        return imageUrl.substring(index + (bucketName + "." + endpoint + "/").length());
    }

    private void ensureOssEnabled() {
        if (ossClient == null) {
            throw new RuntimeException("OSS 未启用，请配置 ALIYUN_OSS_ENABLED=true 及对应密钥");
        }
    }
}
