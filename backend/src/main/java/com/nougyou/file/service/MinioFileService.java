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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioFileService {

    private static final int PRESIGN_EXPIRE_DAYS = 7;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public FileInfo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String bucket = properties.getBucket();
        try {
            ensureBucket(bucket);
            String originalName = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename() : "file";
            String objectName = LocalDate.now() + "/" + UUID.randomUUID() + extension(originalName);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(PRESIGN_EXPIRE_DAYS, TimeUnit.DAYS)
                    .build());
            return new FileInfo(originalName, url, objectName);
        } catch (Exception ex) {
            throw new BusinessException("文件上传失败：" + ex.getMessage());
        }
    }

    public void delete(String filePath) {
        if (!StringUtils.hasText(filePath)) {
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

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String extension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    public record FileInfo(String name, String url, String objectName) {
    }
}
