package com.nougyou.home.service;

import com.nougyou.home.dto.FarmerDashboardVO;
import com.nougyou.home.dto.StockWarnRow;
import com.nougyou.home.dto.StockWarnVO;
import com.nougyou.home.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家经营概览（API-112 / FR-014）。
 * 六类汇总查询拼成一个工作台，全部只读：商品、订单、售后、视频、委托。
 * 成交额口径：已支付且非取消订单的实付金额，按支付时间落在今日 / 本月统计。
 */
@Service
@RequiredArgsConstructor
public class FarmerDashboardService {

    /** 库存预警阈值：低于 10 件即提醒 */
    private static final int STOCK_THRESHOLD = 10;
    private static final int STOCK_WARN_LIMIT = 5;

    private static final int ORDER_WAIT_SHIP = 1;
    private static final int ORDER_WAIT_RECEIVE = 2;
    private static final int ORDER_FINISHED = 3;

    private static final int VIDEO_AUDIT_PENDING = 0;
    private static final int VIDEO_AUDIT_REJECTED = 2;

    private final DashboardMapper dashboardMapper;

    public FarmerDashboardVO overview(Long farmerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        List<StockWarnRow> warnRows = dashboardMapper.stockWarnList(
                farmerId, STOCK_THRESHOLD, STOCK_WARN_LIMIT);
        List<StockWarnVO> stockWarnList = warnRows.stream()
                .map(row -> StockWarnVO.builder()
                        .productId(String.valueOf(row.getProductId()))
                        .name(row.getName())
                        .stock(row.getStock())
                        .shelfStatus(row.getShelfStatus())
                        .build())
                .toList();

        return FarmerDashboardVO.builder()
                .productTotal(dashboardMapper.countProducts(farmerId))
                .onSaleCount(dashboardMapper.countOnSale(farmerId))
                .offSaleCount(dashboardMapper.countOffSale(farmerId))
                .stockWarnCount(dashboardMapper.countStockWarn(farmerId, STOCK_THRESHOLD))
                .orderWaitShip(dashboardMapper.countOrders(farmerId, ORDER_WAIT_SHIP))
                .orderWaitReceive(dashboardMapper.countOrders(farmerId, ORDER_WAIT_RECEIVE))
                .orderFinished(dashboardMapper.countOrders(farmerId, ORDER_FINISHED))
                .todayAmount(amount(dashboardMapper.sumPaidAmount(farmerId, todayStart, tomorrowStart)))
                .monthAmount(amount(dashboardMapper.sumPaidAmount(farmerId, monthStart, nextMonthStart)))
                .afterSalePending(dashboardMapper.countAfterSalePending(farmerId))
                .videoAuditPending(dashboardMapper.countVideos(farmerId, VIDEO_AUDIT_PENDING))
                .videoRejected(dashboardMapper.countVideos(farmerId, VIDEO_AUDIT_REJECTED))
                .entrustCount(dashboardMapper.countActiveEntrusts(farmerId))
                .stockWarnList(stockWarnList)
                .build();
    }

    /**
     * 统一金额精度为两位小数，空值按 0.00 返回，避免前端出现 null。
     */
    private BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
