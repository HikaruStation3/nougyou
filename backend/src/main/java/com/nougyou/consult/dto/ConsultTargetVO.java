package com.nougyou.consult.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 可咨询的农户商家选项。
 */
@Data
@AllArgsConstructor
public class ConsultTargetVO {

    private Long id;
    private String nickname;
    private String avatar;
}
