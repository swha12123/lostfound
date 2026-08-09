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

/**
 * 업로드 이미지의 검증, 저장, 삭제를 담당한다.
 */
@Service
public class FileStoreService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Logger log = LoggerFactory.getLogger(FileStoreService.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String S3_KEY_PREFIX = "lost-items/";

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final Path uploadDir;

    /**
     * 스토리지 서비스를 생성하고 로컬 shadow 디렉터리를 준비한다.
     *
     * @param s3Client S3 클라이언트
     * @param bucketName 대상 버킷명
     * @param region 대상 리전
     * @param uploadDirPath 로컬 업로드 디렉터리 경로
     */
    public FileStoreService(
            S3Client s3Client,
            @Value("${app.s3.bucket:lostfoundstorage}") String bucketName,
            @Value("${spring.cloud.aws.s3.region:us-east-1}") String region,
            @Value("${file.upload-dir:./uploads}") String uploadDirPath
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        ensureUploadDirectoryExists();
    }

    /**
     * 파일이 비어 있지 않고 허용된 이미지 확장자인지 검증한다.
     *
     * @param file 업로드 파일
     */
    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String originalFilename = requireOriginalFilename(file);
        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("jpg, jpeg, png, gif 파일만 업로드할 수 있습니다.");
        }
    }

    /**
     * 파일을 S3에 업로드하고 저장 파일명과 공개 URL을 반환한다.
     *
     * @param file 업로드 파일
     * @return 저장 파일명과 공개 경로
     */
    public String[] storeFile(MultipartFile file) {
        validateFile(file);

        String storedFileName = createStoredFileName(requireOriginalFilename(file));
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storedFileName)
                    .contentType(resolveContentType(file))
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception | SdkClientException | IOException e) {
            log.error("Failed to upload image to S3 bucket {}", bucketName, e);
            throw new ImageUploadFailedException("이미지 업로드에 실패했습니다. S3 설정을 확인해 주세요.", e);
        }

        return new String[]{storedFileName, buildImagePath(storedFileName)};
    }

    /**
     * 저장된 파일을 로컬 shadow 경로와 S3에서 함께 삭제한다.
     *
     * @param storedFileName 저장 파일명
     */
    public void deleteStoredFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        deleteLocalShadowFile(storedFileName);
        deleteFromS3(storedFileName);
    }

    /**
     * 로컬 업로드 디렉터리가 존재하도록 보장한다.
     */
    private void ensureUploadDirectoryExists() {
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리를 준비하지 못했습니다.", e);
        }
    }

    /**
     * 업로드 파일에서 원본 파일명을 읽고 검증한다.
     *
     * @param file 업로드 파일
     * @return 원본 파일명
     */
    private String requireOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }
        return originalFilename;
    }

    /**
     * S3 저장에 사용할 고유 파일명을 생성한다.
     *
     * @param originalFilename 원본 파일명
     * @return 저장 파일명
     */
    private String createStoredFileName(String originalFilename) {
        String extension = getExtension(originalFilename);
        return S3_KEY_PREFIX + UUID.randomUUID() + "." + extension;
    }

    /**
     * 브라우저가 content type을 생략했을 때 기본값으로 보정한다.
     *
     * @param file 업로드 파일
     * @return 업로드에 사용할 content type
     */
    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    /**
     * 저장 파일명으로 공개 이미지 URL을 생성한다.
     *
     * @param storedFileName 저장 파일명
     * @return 공개 이미지 경로
     */
    private String buildImagePath(String storedFileName) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + storedFileName;
    }

    /**
     * 로컬 shadow 파일이 있으면 삭제한다.
     *
     * @param storedFileName 저장 파일명
     */
    private void deleteLocalShadowFile(String storedFileName) {
        try {
            Files.deleteIfExists(this.uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            throw new RuntimeException("기존 업로드 파일 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 저장 파일을 S3에서 삭제한다.
     *
     * @param storedFileName 저장 파일명
     */
    private void deleteFromS3(String storedFileName) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storedFileName)
                .build();
        s3Client.deleteObject(request);
    }

    /**
     * 파일명에서 확장자를 추출한다.
     *
     * @param filename 대상 파일명
     * @return 확장자 또는 {@code null}
     */
    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1);
    }
}
