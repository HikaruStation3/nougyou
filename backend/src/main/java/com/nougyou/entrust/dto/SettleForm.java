package com.nougyou.entrust.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 生成月度结算单入参（API-095）。
 */
@Data
public class SettleForm {

    @NotBlank(message = "结算月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "结算月份格式应为 yyyy-MM")
    private String settleMonth;
}
