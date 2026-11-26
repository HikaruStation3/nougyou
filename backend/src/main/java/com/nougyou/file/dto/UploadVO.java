package com.nougyou.file.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传返回结构（API-113 UploadVO）。
 */
@Data
@Builder
public class UploadVO {

    /** 可访问的文件地址 */
    private String url;

    /** 原始文件名 */
    private String name;

    /** 文件大小（字节） */
    private Long size;

    /** MIME 类型 */
    private String contentType;
}
