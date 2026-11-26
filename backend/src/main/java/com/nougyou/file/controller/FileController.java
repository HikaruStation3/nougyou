package com.nougyou.file.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.file.dto.UploadVO;
import com.nougyou.file.service.MinioFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioFileService fileService;

    /**
     * 上传文件（API-113）：身份证、贫困证明、商品图、头像、短视频统一走此接口。
     * bizType 决定存储目录与校验规则。
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<UploadVO> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "bizType", required = false) String bizType) {
        return ApiResponse.ok(fileService.upload(file, bizType));
    }

    /**
     * 旧版上传入口，保留兼容历史页面与短视频上传；新代码请使用 POST /files/upload。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<MinioFileService.FileInfo> uploadLegacy(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.uploadLegacy(file));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('file:delete')")
    public ApiResponse<Void> delete(@RequestParam(required = false) String filePath) {
        fileService.delete(filePath);
        return ApiResponse.ok();
    }
}
