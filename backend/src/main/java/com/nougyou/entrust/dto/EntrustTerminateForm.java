package com.nougyou.entrust.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 终止委托关系入参（API-092）。
 */
@Data
public class EntrustTerminateForm {

    @Size(max = 200, message = "终止原因最长 200 个字符")
    private String reason;
}
