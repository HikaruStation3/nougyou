package com.nougyou.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布短视频（API-077）：先调上传接口拿到视频与封面地址，再提交本表单，发布后进入待审核。
 */
@Data
public class VideoPublishForm {

    @NotBlank(message = "视频标题不能为空")
    @Size(min = 2, max = 50, message = "视频标题长度需在 2-50 字之间")
    private String title;

    @NotBlank(message = "请上传视频封面")
    @Size(max = 255, message = "封面地址过长")
    private String cover;

    @NotBlank(message = "请上传视频文件")
    @Size(max = 255, message = "视频地址过长")
    private String videoUrl;

    /** 时长（秒），由前端读取视频元数据后传入 */
    @NotNull(message = "视频时长不能为空")
    private Integer duration;

    @Size(max = 500, message = "视频描述最长 500 字")
    private String description;
}
