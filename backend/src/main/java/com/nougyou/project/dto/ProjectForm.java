package com.nougyou.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 农家乐项目表单，申请（API-035）与编辑（API-037）共用。
 * 申请时必填字段由 Service 校验；编辑时仅校验已提供字段的格式。
 */
@Data
public class ProjectForm {

    @Size(min = 5, max = 50, message = "项目名称长度需在 5-50 个字符之间")
    private String title;

    @Size(max = 30, message = "项目类型不能超过 30 个字符")
    private String projectType;

    @Size(max = 255, message = "封面图地址过长")
    private String cover;

    @Size(max = 9, message = "图片最多 9 张")
    private List<String> images;

    @Size(max = 200, message = "项目地址不能超过 200 个字符")
    private String address;

    @Size(max = 1000, message = "项目介绍不能超过 1000 个字符")
    private String intro;

    @Size(max = 100, message = "价格说明不能超过 100 个字符")
    private String priceDesc;

    @Min(value = 1, message = "可接待人数至少为 1")
    @Max(value = 500, message = "可接待人数不能超过 500")
    private Integer capacity;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Size(max = 50, message = "营业时间不能超过 50 个字符")
    private String businessHours;

    /** 编辑时为 true 则把审核状态重置为待审核 */
    private Boolean resubmit;
}
