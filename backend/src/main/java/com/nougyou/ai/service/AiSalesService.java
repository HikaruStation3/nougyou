package com.nougyou.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nougyou.ai.dto.AiAnalysisVO;
import com.nougyou.ai.dto.ProductAnalysisVO;
import com.nougyou.ai.dto.SalesAnalysisRequest;
import com.nougyou.ai.dto.SalesPointVO;
import com.nougyou.ai.dto.SalesStatResultVO;
import com.nougyou.ai.dto.SalesStatVO;
import com.nougyou.ai.dto.row.ProductMonthSaleRow;
import com.nougyou.ai.dto.row.ProductRefundRow;
import com.nougyou.ai.entity.AiChatMessage;
import com.nougyou.ai.entity.AiChatSession;
import com.nougyou.ai.entity.CategoryRef;
import com.nougyou.ai.entity.ProductRef;
import com.nougyou.ai.enums.AiScene;
import com.nougyou.ai.mapper.AiSalesStatMapper;
import com.nougyou.ai.mapper.CategoryRefMapper;
import com.nougyou.ai.mapper.ProductRefMapper;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售数据统计（FR-027）与销售可行性分析（FR-028）。
 * <p>
 * 分析链路：按商家归属校验商品 → 统计近 N 月销量/库存 → 拼装数据上下文 →
 * 调模型（SALE_ANALYSIS 提示词）→ 过管控规则 → 落库，可凭 sessionId 继续追问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSalesService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEFAULT_STAT_DAYS = 30;
    private static final int DEFAULT_ANALYSIS_MONTHS = 6;
    private static final int SIBLING_LIMIT = 5;

    private final ProductRefMapper productMapper;
    private final CategoryRefMapper categoryMapper;
    private final AiSalesStatMapper salesStatMapper;
    private final ChatClient chatClient;
    private final AiConversationStore conversationStore;
    private final AiPromptService promptService;
    private final AiContentGuardService contentGuardService;

    // ------------------------------------------------------------------ 统计

    public SalesStatResultVO stat(Long farmerId, Long productId, String startDate, String endDate,
                                  String groupBy) {
        LocalDate end = parseDate(endDate, LocalDate.now());
        LocalDate start = parseDate(startDate, end.minusDays(DEFAULT_STAT_DAYS - 1L));
        if (start.isAfter(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "统计区间不能超过 366 天");
        }
        if (productId != null) {
            requireOwnedProduct(farmerId, productId);
        }
        return buildStat(farmerId, productId, start, end, normalizeGroupBy(groupBy));
    }

    private SalesStatResultVO buildStat(Long farmerId, Long productId, LocalDate start, LocalDate end,
                                        String groupBy) {
        List<String> months = monthRange(start, end);

        List<ProductRef> products = productMapper.selectList(Wrappers.<ProductRef>lambdaQuery()
                .eq(ProductRef::getFarmerId, farmerId)
                .eq(productId != null, ProductRef::getId, productId)
                .orderByAsc(ProductRef::getId));

        Map<Long, Map<String, Integer>> qtyByProductMonth = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> amountByProductMonth = new HashMap<>();
        for (ProductMonthSaleRow row : salesStatMapper.selectProductMonthSales(
                farmerId, productId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            qtyByProductMonth.computeIfAbsent(row.getProductId(), key -> new HashMap<>())
                    .merge(row.getMonth(), row.getQty() == null ? 0 : row.getQty(), Integer::sum);
            amountByProductMonth.computeIfAbsent(row.getProductId(), key -> new HashMap<>())
                    .merge(row.getMonth(), row.getAmount() == null ? BigDecimal.ZERO : row.getAmount(),
                            BigDecimal::add);
        }

        Map<Long, ProductRefundRow> refunds = new HashMap<>();
        for (ProductRefundRow row : salesStatMapper.selectProductRefunds(
                farmerId, productId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            refunds.put(row.getProductId(), row);
        }

        List<SalesStatVO> items = new ArrayList<>(products.size());
        Map<String, Integer> totalQtyByMonth = new HashMap<>();
        Map<String, BigDecimal> totalAmountByMonth = new HashMap<>();
        int totalQty = 0;
        int totalRefundQty = 0;
        int stockTotal = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (ProductRef product : products) {
            Map<String, Integer> qtyMap = qtyByProductMonth.getOrDefault(product.getId(), Map.of());
            Map<String, BigDecimal> amountMap = amountByProductMonth.getOrDefault(product.getId(), Map.of());

            List<SalesPointVO> trend = new ArrayList<>(months.size());
            int saleQty = 0;
            BigDecimal saleAmount = BigDecimal.ZERO;
            for (String month : months) {
                int qty = qtyMap.getOrDefault(month, 0);
                BigDecimal amount = amountMap.getOrDefault(month, BigDecimal.ZERO);
                trend.add(new SalesPointVO(month, qty, scale(amount)));
                saleQty += qty;
                saleAmount = saleAmount.add(amount);
                totalQtyByMonth.merge(month, qty, Integer::sum);
                totalAmountByMonth.merge(month, amount, BigDecimal::add);
            }

            ProductRefundRow refund = refunds.get(product.getId());
            int refundQty = refund == null || refund.getRefundQty() == null ? 0 : refund.getRefundQty();
            BigDecimal refundAmount = refund == null || refund.getRefundAmount() == null
                    ? BigDecimal.ZERO : refund.getRefundAmount();

            items.add(new SalesStatVO(
                    product.getId(),
                    product.getName(),
                    product.getCover(),
                    product.getStock() == null ? 0 : product.getStock(),
                    product.getUnit(),
                    saleQty,
                    scale(saleAmount),
                    refundQty,
                    scale(refundAmount),
                    trend));

            totalQty += saleQty;
            totalAmount = totalAmount.add(saleAmount);
            totalRefundQty += refundQty;
            totalRefundAmount = totalRefundAmount.add(refundAmount);
            stockTotal += product.getStock() == null ? 0 : product.getStock();
        }

        // 销量高的排前面，便于商家一眼看到主力商品
        items.sort(Comparator.comparingInt(SalesStatVO::saleQty).reversed()
                .thenComparing(SalesStatVO::productId));

        List<SalesPointVO> overallTrend = new ArrayList<>(months.size());
        for (String month : months) {
            overallTrend.add(new SalesPointVO(
                    month,
                    totalQtyByMonth.getOrDefault(month, 0),
                    scale(totalAmountByMonth.getOrDefault(month, BigDecimal.ZERO))));
        }

        return new SalesStatResultVO(
                start.format(DATE_FMT),
                end.format(DATE_FMT),
                groupBy,
                totalQty,
                scale(totalAmount),
                totalRefundQty,
                scale(totalRefundAmount),
                products.size(),
                stockTotal,
                items,
                overallTrend);
    }

    // ------------------------------------------------------------------ 分析

    public AiAnalysisVO analysis(Long farmerId, SalesAnalysisRequest request) {
        ProductRef product = requireOwnedProduct(farmerId, request.productId());
        int months = request.months() == null ? DEFAULT_ANALYSIS_MONTHS : request.months();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months - 1L).withDayOfMonth(1);
        SalesStatVO stat = buildStat(farmerId, product.getId(), start, end, "product")
                .items().stream()
                .filter(item -> item.productId().equals(product.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "商品暂无销售数据"));

        List<ProductAnalysisVO> analysisItems = analyseProducts(farmerId, product, stat, end, months);
        String dataContext = buildDataContext(product, stat, analysisItems, start, end);
        String question = "请分析「" + product.getName() + "」后续是否值得继续备货，"
                + "按【结论】【依据】【建议动作】三段输出。";

        AiChatSession session = resolveSession(farmerId, request.sessionId(), product.getName());
        List<Message> history = conversationStore.loadHistory(session.getId());
        conversationStore.appendMessage(session.getId(), "user", question + "\n\n" + dataContext, false);

        String systemPrompt = promptService.systemPrompt(AiScene.SALE_ANALYSIS)
                + "\n\n补充信息：今天是 " + end + "。";

        String rawContent;
        int tokenCount = 0;
        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(history)
                    .user(question + "\n\n" + dataContext)
                    .call()
                    .chatResponse();
            rawContent = response == null || response.getResult() == null
                    ? "" : response.getResult().getOutput().getText();
            tokenCount = extractTokens(response);
        } catch (Exception ex) {
            log.error("销售可行性分析调用失败 farmerId={} productId={}", farmerId, product.getId(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 分析服务暂时不可用，请稍后重试");
        }

        AiContentGuardService.GuardResult guard = contentGuardService.inspect(rawContent);
        AiChatMessage reply = conversationStore.appendMessage(
                session.getId(), "assistant", guard.content(), guard.flagged());
        conversationStore.refreshSession(session.getId());

        return new AiAnalysisVO(session.getId(), reply.getId(), guard.content(), analysisItems, tokenCount);
    }

    private AiChatSession resolveSession(Long userId, Long sessionId, String productName) {
        if (sessionId == null) {
            return conversationStore.createSession(userId, AiScene.SALE_ANALYSIS,
                    "可行性分析：" + productName);
        }
        AiChatSession session = conversationStore.requireOwnedSession(userId, sessionId);
        if (!AiScene.SALE_ANALYSIS.name().equalsIgnoreCase(session.getSceneCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该会话不是销售可行性分析会话");
        }
        return session;
    }

    /**
     * 目标商品 + 同品类商品（最多 5 个）的规则化分析，作为 AI 结论的结构化补充。
     */
    private List<ProductAnalysisVO> analyseProducts(Long farmerId, ProductRef target, SalesStatVO targetStat,
                                                    LocalDate end, int months) {
        List<ProductRef> sameCategory = productMapper.selectList(Wrappers.<ProductRef>lambdaQuery()
                .eq(ProductRef::getFarmerId, farmerId)
                .eq(ProductRef::getCategoryId, target.getCategoryId())
                .ne(ProductRef::getId, target.getId())
                .orderByDesc(ProductRef::getSaleCount)
                .last("LIMIT " + SIBLING_LIMIT));

        List<ProductAnalysisVO> result = new ArrayList<>();
        result.add(toAnalysis(target, targetStat));

        if (!sameCategory.isEmpty()) {
            LocalDate start = end.minusMonths(months - 1L).withDayOfMonth(1);
            List<Long> ids = sameCategory.stream().map(ProductRef::getId).toList();
            Map<Long, SalesStatVO> siblingStats = new HashMap<>();
            for (SalesStatVO item : buildStat(farmerId, null, start, end, "product").items()) {
                if (ids.contains(item.productId())) {
                    siblingStats.put(item.productId(), item);
                }
            }
            for (ProductRef sibling : sameCategory) {
                SalesStatVO stat = siblingStats.get(sibling.getId());
                if (stat != null) {
                    result.add(toAnalysis(sibling, stat));
                }
            }
        }
        return result;
    }

    private ProductAnalysisVO toAnalysis(ProductRef product, SalesStatVO stat) {
        String direction = trendDirection(stat.trend());
        return new ProductAnalysisVO(product.getId(), product.getName(), stat.saleQty(),
                direction, suggestion(product, stat, direction));
    }

    private String trendDirection(List<SalesPointVO> trend) {
        if (trend.isEmpty()) {
            return "flat";
        }
        int middle = Math.max(1, trend.size() / 2);
        double firstHalf = trend.subList(0, middle).stream().mapToInt(SalesPointVO::qty).average().orElse(0);
        double secondHalf = trend.subList(middle, trend.size()).stream()
                .mapToInt(SalesPointVO::qty).average().orElse(0);

        if (firstHalf == 0 && secondHalf == 0) {
            return "flat";
        }
        if (firstHalf == 0) {
            // 从零起步且只有零星成交时不判为上升，避免噪声被当成趋势
            return secondHalf >= 2 ? "up" : "flat";
        }
        double ratio = (secondHalf - firstHalf) / firstHalf;
        if (ratio > 0.15) {
            return "up";
        }
        if (ratio < -0.15) {
            return "down";
        }
        return "flat";
    }

    private String suggestion(ProductRef product, SalesStatVO stat, String direction) {
        int stock = product.getStock() == null ? 0 : product.getStock();
        boolean onShelf = product.getShelfStatus() == null || product.getShelfStatus() == 1;
        if (stat.saleQty() == 0) {
            return stock > 0 ? "近几月无成交且有库存，建议促销清库存或下架" : "近几月无成交，建议评估是否下架";
        }

        int days = Math.max(1, stat.trend().size() * 30);
        double avgDaily = (double) stat.saleQty() / days;
        long turnoverDays = avgDaily <= 0 ? Long.MAX_VALUE : Math.round(stock / avgDaily);

        if (turnoverDays > 60) {
            return turnoverDays > 999
                    ? "库存周转超过 999 天，明显偏慢，建议促销清库存或减少上架量"
                    : "库存周转约 " + turnoverDays + " 天，偏慢，建议减少上架量";
        }
        if ("up".equals(direction) && turnoverDays < 15) {
            return "需求上升且周转快，可适度补货";
        }
        if ("down".equals(direction)) {
            return "销量下行，建议降低备货量并观察两周";
        }
        if (!onShelf) {
            return "已下架但仍有成交，可考虑恢复上架";
        }
        return "销量平稳，可维持现有库存";
    }

    private String buildDataContext(ProductRef product, SalesStatVO stat,
                                    List<ProductAnalysisVO> items, LocalDate start, LocalDate end) {
        CategoryRef category = categoryMapper.selectById(product.getCategoryId());
        int days = (int) (ChronoUnit.DAYS.between(start, end) + 1);
        double avgDaily = days <= 0 ? 0 : (double) stat.saleQty() / days;
        long turnoverDays = avgDaily <= 0 ? -1 : Math.round((product.getStock() == null ? 0 : product.getStock()) / avgDaily);

        StringBuilder sb = new StringBuilder();
        sb.append("【系统提供的销售数据，请勿编造此处没有的数字】\n");
        sb.append("分析对象：").append(product.getName())
                .append("｜品类：").append(category == null ? "未分类" : category.getName())
                .append("｜当前库存：").append(product.getStock() == null ? 0 : product.getStock())
                .append(StringUtils.hasText(product.getUnit()) ? product.getUnit() : "件")
                .append("｜状态：").append(product.getShelfStatus() != null && product.getShelfStatus() == 0 ? "已下架" : "上架中")
                .append('\n');
        sb.append("统计区间：").append(start).append(" 至 ").append(end)
                .append("（约 ").append(days).append(" 天）\n");
        sb.append("区间销量：").append(stat.saleQty()).append(" 件，销售额 ")
                .append(stat.saleAmount()).append(" 元，退单 ").append(stat.refundQty())
                .append(" 件，退款 ").append(stat.refundAmount()).append(" 元\n");

        sb.append("月销量：");
        for (SalesPointVO point : stat.trend()) {
            sb.append(point.month()).append(' ').append(point.qty()).append(" 件；");
        }
        sb.append('\n');
        sb.append("日均销量：").append(String.format("%.2f", avgDaily)).append(" 件")
                .append("，库存周转天数：")
                .append(turnoverDays < 0 ? "无法测算（无成交）"
                        : turnoverDays > 999 ? "超过 999 天（库存明显积压）" : turnoverDays + " 天")
                .append('\n');

        List<ProductAnalysisVO> siblings = items.stream()
                .filter(item -> !item.productId().equals(product.getId()))
                .toList();
        if (!siblings.isEmpty()) {
            sb.append("同品类其他商品：");
            for (ProductAnalysisVO sibling : siblings) {
                sb.append(sibling.productName()).append("（近 ").append(stat.trend().size())
                        .append(" 个月 ").append(sibling.saleQty()).append(" 件，走势 ")
                        .append("up".equals(sibling.trendDirection()) ? "上升"
                                : "down".equals(sibling.trendDirection()) ? "下降" : "平稳")
                        .append("）；");
            }
            sb.append('\n');
        }
        sb.append("统计口径：仅统计已支付且未取消订单；退单指售后已完成的退款单。");
        return sb.toString();
    }

    // ------------------------------------------------------------------ 工具

    private ProductRef requireOwnedProduct(Long farmerId, Long productId) {
        ProductRef product = productId == null ? null : productMapper.selectById(productId);
        if (product == null || !product.getFarmerId().equals(farmerId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品不存在或不属于当前商家");
        }
        return product;
    }

    private static List<String> monthRange(LocalDate start, LocalDate end) {
        List<String> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            months.add(cursor.toString());
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private static String normalizeGroupBy(String groupBy) {
        return "month".equalsIgnoreCase(groupBy == null ? null : groupBy.trim()) ? "month" : "product";
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式应为 yyyy-MM-dd");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static int extractTokens(ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return 0;
        }
        Integer total = response.getMetadata().getUsage().getTotalTokens();
        return total == null ? 0 : total;
    }
}
