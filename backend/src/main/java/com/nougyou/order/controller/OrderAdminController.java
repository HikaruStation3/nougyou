package com.nougyou.order.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.order.dto.OrderPageQuery;
import com.nougyou.order.dto.OrderVO;
import com.nougyou.order.service.OrderService;
import com.nougyou.order.util.OrderExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 后台订单（API-060）：管理员查询与导出订单，只有查询权，不改金额与状态。
 */
@RestController
@RequestMapping({"/api/v1/admin/orders", "/api/admin/orders"})
@RequiredArgsConstructor
public class OrderAdminController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<Integer, String> STATUS_TEXT = Map.of(
            0, "待付款", 1, "待发货", 2, "待收货", 3, "已完成", 4, "已取消", 5, "售后中");

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:adminList')")
    public ApiResponse<PageResult<OrderVO>> page(OrderPageQuery query) {
        return ApiResponse.ok(orderService.adminPage(query));
    }

    /**
     * API-060 导出订单：筛选条件与查询一致，不分页，返回 .xlsx 报表。
     * 手机号明文（与订单详情一致），仅具备 order:adminList 的管理员可下载。
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('order:adminList')")
    public ResponseEntity<byte[]> export(OrderPageQuery query) {
        List<OrderVO> orders = orderService.adminExport(query);
        byte[] body = OrderExcelExporter.write(orders, filterSummary(query));

        String stamp = LocalDateTime.now().format(FILE_STAMP);
        String asciiName = "orders_" + stamp + ".xlsx";
        String utf8Name = URLEncoder.encode("订单列表_" + stamp + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + utf8Name)
                .header("X-Export-Count", String.valueOf(orders.size()))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /** 把本次导出的筛选条件写进文件标题下方，便于核对这份表和页面查询口径是否一致 */
    private static String filterSummary(OrderPageQuery q) {
        List<String> parts = new ArrayList<>();
        if (q.getOrderNo() != null && !q.getOrderNo().isBlank()) {
            parts.add("订单号=" + q.getOrderNo());
        }
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            parts.add("关键词=" + q.getKeyword());
        }
        if (q.getBuyerId() != null) {
            parts.add("买家ID=" + q.getBuyerId());
        }
        if (q.getFarmerId() != null) {
            parts.add("商家ID=" + q.getFarmerId());
        }
        if (q.getOrderStatus() != null) {
            parts.add("订单状态=" + STATUS_TEXT.getOrDefault(q.getOrderStatus(),
                    String.valueOf(q.getOrderStatus())));
        }
        if (q.getStartTime() != null) {
            parts.add("下单起=" + q.getStartTime().format(TIME));
        }
        if (q.getEndTime() != null) {
            parts.add("下单止=" + q.getEndTime().format(TIME));
        }
        return parts.isEmpty() ? "全部订单" : String.join("，", parts);
    }
}
