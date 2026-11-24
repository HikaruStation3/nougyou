package com.nougyou.file.service;

import com.nougyou.common.exception.BusinessException;
import com.nougyou.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储：优先使用 MinIO（NFR-011），MinIO 不可用时退回本地磁盘，
 * 保证在没有启动对象存储的开发机上依然可以上传短视频并本地播放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    private static final int PRESIGN_EXPIRE_DAYS = 7;
    private static final String LOCAL_URL_PREFIX = "/api/v1/files/local/";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Value("${nougyou.file.local-dir:./uploads}")
    private String localDir;

    public FileInfo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        String objectName = LocalDate.now() + "/" + UUID.randomUUID() + extension(originalName);
        try {
            String url = uploadToMinio(file, objectName);
            return new FileInfo(originalName, url, objectName);
        } catch (Exception ex) {
            log.warn("MinIO 不可用（{}），改用本地磁盘存储", ex.getMessage());
            return uploadToLocal(file, originalName, objectName);
        }
    }

    public void delete(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        if (filePath.startsWith(LOCAL_URL_PREFIX)) {
            deleteLocal(filePath.substring(LOCAL_URL_PREFIX.length()));
            return;
        }
        String objectName = filePath;
        String marker = "/" + properties.getBucket() + "/";
        int index = filePath.indexOf(marker);
        if (index >= 0) {
            objectName = filePath.substring(index + marker.length());
        }
        int queryIndex = objectName.indexOf('?');
        if (queryIndex >= 0) {
            objectName = objectName.substring(0, queryIndex);
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("文件删除失败：" + ex.getMessage());
        }
    }

    /* ------------------------------ MinIO ------------------------------ */

    private String uploadToMinio(MultipartFile file, String objectName) throws Exception {
        String bucket = properties.getBucket();
        ensureBucket(bucket);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry(PRESIGN_EXPIRE_DAYS, TimeUnit.DAYS)
                .build());
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    /* ------------------------------ 本地兜底 ------------------------------ */

    private FileInfo uploadToLocal(MultipartFile file, String originalName, String objectName) {
        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        Path target = root.resolve(objectName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败：" + ex.getMessage());
        }
        return new FileInfo(originalName, LOCAL_URL_PREFIX + objectName, objectName);
    }

    private void deleteLocal(String objectName) {
        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        Path target = root.resolve(objectName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的文件路径");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException("文件删除失败：" + ex.getMessage());
        }
    }

    private String extension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    public record FileInfo(String name, String url, String objectName) {
    }
}
