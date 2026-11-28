package com.nougyou.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;

    /** 浏览器可访问的地址；为空时回退到 endpoint（容器部署时两者不同） */
    private String publicEndpoint;

    private String accessKey;
    private String secretKey;
    private String bucket;
}
