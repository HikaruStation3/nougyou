package com.nougyou.consult.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 结束会话入参（API-074），可选处理小结。
 */
@Data
public class ServiceCloseRequest {

    @Size(max = 200, message = "处理小结不能超过 200 个字符")
    private String summary;
}
