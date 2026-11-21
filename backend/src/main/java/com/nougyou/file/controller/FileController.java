package com.nougyou.file.controller;

import com.nougyou.common.api.ApiResponse;
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

    @PostMapping
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResponse<MinioFileService.FileInfo> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(file));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('file:delete')")
    public ApiResponse<Void> delete(@RequestParam(required = false) String filePath) {
        fileService.delete(filePath);
        return ApiResponse.ok();
    }
}
