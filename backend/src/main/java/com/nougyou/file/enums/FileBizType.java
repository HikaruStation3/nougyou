package com.nougyou.file.enums;

import com.nougyou.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * 上传文件的业务类型（API-113）。
 * <p>
 * bizType 决定存储目录、允许的扩展名与大小上限：
 * 身份证 / 贫困证明 / 商品图 / 头像 只收 jpg、jpeg、png，单文件不超过 5MB；
 * 短视频只收 mp4，单文件不超过 100MB。
 */
@Getter
public enum FileBizType {

    IDCARD("idcard", "身份证", Set.of("jpg", "jpeg", "png"), 5L * 1024 * 1024),
    PROOF("proof", "贫困证明", Set.of("jpg", "jpeg", "png"), 5L * 1024 * 1024),
    PRODUCT("product", "商品图", Set.of("jpg", "jpeg", "png"), 5L * 1024 * 1024),
    VIDEO("video", "短视频", Set.of("mp4"), 100L * 1024 * 1024),
    AVATAR("avatar", "头像", Set.of("jpg", "jpeg", "png"), 5L * 1024 * 1024);

    private final String code;
    private final String label;
    private final Set<String> extensions;
    private final long maxSize;

    FileBizType(String code, String label, Set<String> extensions, long maxSize) {
        this.code = code;
        this.label = label;
        this.extensions = extensions;
        this.maxSize = maxSize;
    }

    public static FileBizType of(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("请选择文件业务类型（idcard/proof/product/video/avatar）");
        }
        String normalized = code.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "不支持的业务类型：" + code + "，可选 idcard/proof/product/video/avatar"));
    }

    /** 前端提示用：jpg/jpeg/png 或 mp4（固定顺序，避免 Set 遍历顺序不稳定） */
    public String extensionText() {
        return isVideo() ? "mp4" : "jpg/jpeg/png";
    }

    /** 前端提示用：5MB / 100MB */
    public String maxSizeText() {
        return (maxSize / 1024 / 1024) + "MB";
    }

    public boolean isVideo() {
        return this == VIDEO;
    }
}
