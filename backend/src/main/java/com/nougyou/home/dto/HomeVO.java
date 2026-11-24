package com.nougyou.home.dto;

import com.nougyou.product.dto.ProductVO;
import com.nougyou.project.dto.ProjectVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 首页聚合数据（API-111 / FR-002）。
 * 一次请求返回政策、资讯、农家乐与热销商品，全部为公开数据，游客可访问。
 */
@Data
@Builder
public class HomeVO {

    /** 最新扶贫政策，最多 5 条 */
    private List<HomePolicyVO> policies;
    /** 最新资讯，最多 8 条 */
    private List<HomeNewsVO> news;
    /** 推荐农家乐项目，最多 4 条 */
    private List<ProjectVO> projects;
    /** 热销农产品，按累计销量倒序，最多 8 条 */
    private List<ProductVO> hotProducts;
}
