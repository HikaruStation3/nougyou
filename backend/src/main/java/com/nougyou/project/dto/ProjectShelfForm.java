package com.nougyou.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectShelfForm {

    /** 目标状态：0下架 1上架 */
    @NotNull(message = "目标状态不能为空")
    private Integer shelfStatus;
}
