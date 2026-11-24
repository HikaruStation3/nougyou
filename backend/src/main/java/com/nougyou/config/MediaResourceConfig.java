package com.nougyou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 媒体静态资源映射。
 * <p>
 * 1）{@code /api/v1/media/**} 指向打包在 resources/static/media 下的演示素材，
 * 让种子数据里的短视频在没有 MinIO 的机器上也能直接播放；
 * 2）{@code /api/v1/files/local/**} 指向本地兜底上传目录，
 * MinIO 不可用时 MinioFileService 会把文件落在这里，{@code <video>} 也能按 Range 请求拖动进度条。
 */
@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final Path localRoot;

    public MediaResourceConfig(@Value("${nougyou.file.local-dir:./uploads}") String localDir) {
        this.localRoot = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.localRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建本地上传目录：" + this.localRoot, ex);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/media/**")
                .addResourceLocations("classpath:/static/media/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/api/v1/files/local/**")
                .addResourceLocations(ensureTrailingSlash(localRoot.toUri().toString()))
                .setCachePeriod(0);
    }

    private String ensureTrailingSlash(String location) {
        return location.endsWith("/") ? location : location + "/";
    }
}
