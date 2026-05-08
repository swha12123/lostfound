package com.example.lostfound.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStoreService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Logger log = LoggerFactory.getLogger(FileStoreService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final Path uploadDir;

    public FileStoreService(
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucketName,
            @Value("${spring.cloud.aws.s3.region}") String region,
            @Value("${file.upload-dir:./uploads}") String uploadDirPath
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리를 준비하지 못했습니다.", e);
        }
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("jpg, jpeg, png, gif 파일만 업로드할 수 있습니다.");
        }
    }

    public String[] storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFileName = "lost-items/" + UUID.randomUUID() + "." + extension;
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storedFileName)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception | SdkClientException | IOException e) {
            log.error("Failed to upload image to S3 bucket {}", bucketName, e);
            throw new IllegalArgumentException("이미지 업로드에 실패했습니다. S3 설정을 확인해 주세요.", e);
        }

        String imagePath = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + storedFileName;
        return new String[]{storedFileName, imagePath};
    }

    public void deleteStoredFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(this.uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            throw new RuntimeException("기존 업로드 파일 삭제에 실패했습니다.", e);
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storedFileName)
                .build();
        s3Client.deleteObject(request);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1);
    }
}
