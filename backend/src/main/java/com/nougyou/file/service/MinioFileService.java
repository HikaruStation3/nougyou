package com.nougyou.file.service;

import com.nougyou.common.exception.BusinessException;
import com.nougyou.config.MinioProperties;
import com.nougyou.file.dto.UploadVO;
import com.nougyou.file.enums.FileBizType;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 文件存储：优先使用 MinIO（NFR-011），MinIO 不可用时退回本地磁盘，
 * 保证在没有启动对象存储的开发机上依然可以上传证件、商品图与短视频。
 * <p>
 * 桶设为公共读，返回固定访问地址，避免预签名地址过期导致图片失效。
 * 对象路径按业务类型分目录：{@code idcard/2026/11/uuid.jpg}，便于按用途归档与清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    private static final String LOCAL_URL_PREFIX = "/api/v1/files/local/";
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /** 扩展名与 MIME 的固定映射，避免信任客户端传来的 Content-Type */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "mp4", "video/mp4");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Value("${nougyou.file.local-dir:./uploads}")
    private String localDir;

    /**
     * 按业务类型上传文件（API-113）。
     */
    public UploadVO upload(MultipartFile file, String bizTypeCode) {
        FileBizType bizType = FileBizType.of(bizTypeCode);
        StoredFile stored = store(file, bizType);
        return UploadVO.builder()
                .url(stored.url())
                .name(stored.originalName())
                .size(stored.size())
                .contentType(stored.contentType())
                .build();
    }

    /**
     * 旧版上传接口（POST /api/v1/files）保留兼容：
     * 视频按 mp4 归档到 video 目录，其余图片统一归档到 product 目录。
     */
    public FileInfo uploadLegacy(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String extension = extension(originalName(file)).toLowerCase();
        FileBizType bizType = "mp4".equals(extension) ? FileBizType.VIDEO : FileBizType.PRODUCT;
        StoredFile stored = store(file, bizType);
        return new FileInfo(stored.originalName(), stored.url(), stored.objectName());
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

    /* ------------------------------ 校验与存储 ------------------------------ */

    private StoredFile store(MultipartFile file, FileBizType bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = originalName(file);
        String extension = extension(originalName).toLowerCase();
        if (!bizType.getExtensions().contains(extension)) {
            throw new BusinessException(bizType.getLabel() + "仅支持 " + bizType.extensionText() + " 格式");
        }
        if (file.getSize() > bizType.getMaxSize()) {
            throw new BusinessException(bizType.getLabel() + "大小不能超过 " + bizType.maxSizeText());
        }

        String objectName = bizType.getCode() + "/" + LocalDate.now().format(MONTH_FORMAT)
                + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        try {
            String url = uploadToMinio(file, objectName, contentType);
            return new StoredFile(originalName, objectName, url, file.getSize(), contentType);
        } catch (Exception ex) {
            log.warn("MinIO 不可用（{}），改用本地磁盘存储", ex.getMessage());
            return uploadToLocal(file, originalName, objectName, contentType);
        }
    }

    private String originalName(MultipartFile file) {
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        // 去掉 IE / 部分客户端带上的完整路径，避免文件名里混入目录
        int slash = Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\'));
        return slash >= 0 ? originalName.substring(slash + 1) : originalName;
    }

    /* ------------------------------ MinIO ------------------------------ */

    private String uploadToMinio(MultipartFile file, String objectName, String contentType) throws Exception {
        String bucket = properties.getBucket();
        ensureBucket(bucket);
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        }
        return publicUrl(objectName);
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        // 桶设为公共读，返回固定访问地址，避免预签名地址过期导致图片失效
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(publicReadPolicy(bucket))
                .build());
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    private String publicUrl(String objectName) {
        String endpoint = properties.getEndpoint();
        if (endpoint != null && endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + properties.getBucket() + "/" + objectName;
    }

    /* ------------------------------ 本地兜底 ------------------------------ */

    private StoredFile uploadToLocal(MultipartFile file, String originalName, String objectName,
                                     String contentType) {
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
        return new StoredFile(originalName, objectName, LOCAL_URL_PREFIX + objectName,
                file.getSize(), contentType);
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

    /**
     * 取扩展名（不含点）；没有扩展名时返回空字符串。
     */
    private String extension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 && dotIndex < fileName.length() - 1
                ? fileName.substring(dotIndex + 1) : "";
    }

    private record StoredFile(String originalName, String objectName, String url,
                              long size, String contentType) {
    }

    public record FileInfo(String name, String url, String objectName) {
    }
}
