package com.nougyou.entrust.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 可选的受托方（农户商家），用于建立委托关系时选择同村受托人。
 */
@Data
@Builder
public class EntrustAgentVO {

    private String id;
    private String name;
    private String shopName;
}
