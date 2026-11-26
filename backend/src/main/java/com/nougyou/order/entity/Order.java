package com.nougyou.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表，按「买家 - 商家」维度拆单（FR-007 / FR-018）。
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long farmerId;
    private BigDecimal totalAmount;
    private BigDecimal freight;
    private BigDecimal payAmount;
    /** 0 未支付 / 1 已支付 / 2 已退款 */
    private Integer payStatus;
    /** 1 微信 / 2 支付宝 / 3 模拟支付 */
    private Integer payType;
    private LocalDateTime payTime;
    /** 支付宝交易号，沙箱/生产支付成功后回填，模拟支付为空 */
    private String tradeNo;
    /** 0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 / 5 售后中 */
    private Integer orderStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private String logisticsCompany;
    private String logisticsNo;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
