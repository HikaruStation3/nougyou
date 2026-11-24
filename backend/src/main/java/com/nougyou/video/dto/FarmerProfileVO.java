package com.nougyou.video.dto;

import com.nougyou.product.dto.ProductVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 商家主页（API-085）：资料 + 在售商品 + 已发布视频。
 */
@Data
@Builder
public class FarmerProfileVO {

    private Long farmerId;

    private String realName;

    private String shopName;

    private String avatar;

    /** 所属村组：当前库未单独存字段，取自店铺名，缺失时为 null */
    private String village;

    /** 当前用户是否已关注 */
    private Boolean isFollowed;

    /** 粉丝数 */
    private Integer followCount;

    /** 在售商品数 */
    private Integer productCount;

    /** 已发布视频数 */
    private Integer videoCount;

    /** 在售商品列表，取前 10 条 */
    private List<ProductVO> products;

    /** 已发布视频列表，取前 10 条 */
    private List<VideoVO> videos;
}
