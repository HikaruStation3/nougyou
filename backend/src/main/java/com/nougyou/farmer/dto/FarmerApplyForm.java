package com.nougyou.farmer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FarmerApplyForm {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名不能超过 50 个字符")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "所属村不能为空")
    @Size(max = 50, message = "所属村不能超过 50 个字符")
    private String village;

    @Size(max = 50, message = "贫困户编号不能超过 50 个字符")
    private String povertyCardNo;

    @Size(max = 20, message = "贫困属性不能超过 20 个字符")
    private String povertyLevel;

    @NotNull(message = "家庭人口数不能为空")
    @Min(value = 1, message = "家庭人口数至少为 1")
    private Integer householdNum;

    @Size(max = 120, message = "拟经营品类不能超过 120 个字符")
    private String mainProduct;

    @Size(max = 255, message = "身份证正面照地址过长")
    private String idCardFront;

    @Size(max = 255, message = "身份证反面照地址过长")
    private String idCardBack;

    @Size(max = 255, message = "贫困证明地址过长")
    private String povertyProof;

    @Size(max = 500, message = "申请说明不能超过 500 个字符")
    private String applyDesc;
}
