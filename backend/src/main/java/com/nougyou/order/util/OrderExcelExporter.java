package com.nougyou.order.util;

import com.nougyou.order.dto.OrderItemVO;
import com.nougyou.order.dto.OrderVO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API-060 后台订单导出：生成 .xlsx 报表。
 * <p>
 * 版式（三行表头区 + 数据区）：
 * <pre>
 * 第 1 行  钟山镇扶贫助农系统 · 订单列表       合并居中、16 号加粗
 * 第 2 行  导出时间：… 条数：… 筛选条件：…    合并居中、灰色小字
 * 第 3 行  21 列字段名                        深绿底白字加粗、带筛选箭头
 * 第 4 行起 订单数据，一单一行                金额右对齐两位小数，长文本自动换行
 * </pre>
 * 前三行冻结，滚动时表头始终可见。
 */
public final class OrderExcelExporter {

    /** 报表标题，写在第一行 */
    public static final String TITLE = "钟山镇扶贫助农系统 · 订单列表";

    private static final String SHEET_NAME = "订单列表";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int HEADER_ROW = 2;
    private static final int FIRST_DATA_ROW = 3;

    private static final String[] HEADER = {
            "订单号", "下单时间", "订单状态", "支付状态", "支付方式",
            "买家", "商家店铺", "商家账号", "商品明细",
            "商品总额", "运费", "实付金额",
            "收货人", "联系电话", "收货地址", "买家备注",
            "支付时间", "发货时间", "物流公司", "物流单号", "完成时间"
    };

    /** 列宽（单位：字符数），与 HEADER 一一对应 */
    private static final int[] WIDTH = {
            22, 20, 10, 10, 10,
            10, 16, 14, 38,
            12, 10, 12,
            10, 14, 36, 20,
            20, 20, 12, 20, 20
    };

    /** 需要居中的列：订单状态 / 支付状态 / 支付方式 */
    private static final int[] CENTER_COLS = {2, 3, 4};
    /** 金额列：商品总额 / 运费 / 实付金额 */
    private static final int[] MONEY_COLS = {9, 10, 11};
    /** 长文本列：商品明细 / 收货地址 / 买家备注 */
    private static final int[] WRAP_COLS = {8, 14, 15};

    private static final Map<Integer, String> ORDER_STATUS = Map.of(
            0, "待付款", 1, "待发货", 2, "待收货", 3, "已完成", 4, "已取消", 5, "售后中");
    private static final Map<Integer, String> PAY_STATUS = Map.of(
            0, "未支付", 1, "已支付", 2, "已退款");
    private static final Map<Integer, String> PAY_TYPE = Map.of(
            1, "微信支付", 2, "支付宝", 3, "模拟支付");

    private OrderExcelExporter() {
    }

    public static byte[] write(List<OrderVO> orders, String filterSummary) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(SHEET_NAME);
            Styles styles = new Styles(wb);

            for (int i = 0; i < WIDTH.length; i++) {
                sheet.setColumnWidth(i, WIDTH[i] * 256);
            }

            writeTitle(sheet, styles, orders == null ? 0 : orders.size(), filterSummary);
            writeHeader(sheet, styles);

            int rowIndex = FIRST_DATA_ROW;
            if (orders != null) {
                for (OrderVO order : orders) {
                    writeDataRow(sheet.createRow(rowIndex), styles, order);
                    rowIndex++;
                }
            }

            // 冻结前 3 行（标题 / 说明 / 表头），滚动时表头不动
            sheet.createFreezePane(0, FIRST_DATA_ROW);
            // 表头行的筛选箭头，范围随数据延伸
            sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, Math.max(rowIndex - 1, HEADER_ROW),
                    0, HEADER.length - 1));

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成订单导出文件失败", e);
        }
    }

    private static void writeTitle(Sheet sheet, Styles styles, int count, String filterSummary) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(TITLE);
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADER.length - 1));

        Row metaRow = sheet.createRow(1);
        metaRow.setHeightInPoints(20);
        Cell metaCell = metaRow.createCell(0);
        metaCell.setCellValue(meta(count, filterSummary));
        metaCell.setCellStyle(styles.meta);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADER.length - 1));
    }

    private static void writeHeader(Sheet sheet, Styles styles) {
        Row row = sheet.createRow(HEADER_ROW);
        row.setHeightInPoints(24);
        for (int i = 0; i < HEADER.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADER[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private static void writeDataRow(Row row, Styles styles, OrderVO o) {
        String items = items(o.getItems());
        Object[] cells = {
                o.getOrderNo(),
                time(o.getCreateTime()),
                label(ORDER_STATUS, o.getOrderStatus()),
                label(PAY_STATUS, o.getPayStatus()),
                label(PAY_TYPE, o.getPayType()),
                o.getBuyerName(),
                o.getShopName(),
                o.getFarmerName(),
                items,
                o.getTotalAmount(),
                o.getFreight(),
                o.getPayAmount(),
                o.getReceiverName(),
                o.getReceiverPhone(),
                o.getReceiverAddress(),
                o.getRemark(),
                time(o.getPayTime()),
                time(o.getShipTime()),
                o.getLogisticsCompany(),
                o.getLogisticsNo(),
                time(o.getFinishTime())
        };

        for (int i = 0; i < cells.length; i++) {
            Cell cell = row.createCell(i);
            Object value = cells[i];
            if (value instanceof BigDecimal money) {
                cell.setCellValue(money.doubleValue());
                cell.setCellStyle(styles.money);
            } else {
                cell.setCellValue(value == null ? "" : value.toString());
                cell.setCellStyle(styles.textFor(i));
            }
        }

        // 商品明细/地址/备注可能很长，按内容粗略估算行高，避免只显示一行
        int lines = Math.max(estimateLines(items, 34), estimateLines(o.getReceiverAddress(), 32));
        lines = Math.max(lines, estimateLines(o.getRemark(), 18));
        row.setHeightInPoints(Math.min(lines, 5) * 15f + 3f);
    }

    private static int estimateLines(String text, int charsPerLine) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(text.length() / (double) charsPerLine));
    }

    /** 导出说明行：交代这份文件是什么时候、按什么条件导出的 */
    private static String meta(int count, String filterSummary) {
        String filter = (filterSummary == null || filterSummary.isBlank()) ? "全部订单" : filterSummary;
        return "导出时间：" + LocalDateTime.now().format(TIME)
                + "    数据条数：" + count + " 条"
                + "    筛选条件：" + filter;
    }

    /** 一个订单的商品合并成一格，如「钟山米粉×2；高山绿茶×1」 */
    private static String items(List<OrderItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(item -> item.getProductName() + "×" + item.getQuantity())
                .collect(Collectors.joining("；"));
    }

    /** 未支付订单的 pay_type 可能为 null，Map.of 不接受 null 键，这里统一兜底 */
    private static String label(Map<Integer, String> dict, Integer code) {
        return code == null ? "" : dict.getOrDefault(code, "");
    }

    private static String time(LocalDateTime value) {
        return value == null ? "" : value.format(TIME);
    }

    private static boolean contains(int[] arr, int value) {
        for (int item : arr) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    /** 样式集中创建一次，POI 的 CellStyle 不能跨工作簿复用 */
    private static final class Styles {

        private final CellStyle title;
        private final CellStyle meta;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle textCenter;
        private final CellStyle textWrap;
        private final CellStyle money;

        private Styles(XSSFWorkbook wb) {
            Font titleFont = font(wb, 16, true, IndexedColors.BLACK);
            Font metaFont = font(wb, 10, false, IndexedColors.GREY_50_PERCENT);
            Font headerFont = font(wb, 10, true, IndexedColors.WHITE);
            Font bodyFont = font(wb, 10, false, IndexedColors.BLACK);

            title = wb.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            meta = wb.createCellStyle();
            meta.setFont(metaFont);
            meta.setAlignment(HorizontalAlignment.CENTER);
            meta.setVerticalAlignment(VerticalAlignment.CENTER);

            header = wb.createCellStyle();
            header.setFont(headerFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            border(header, IndexedColors.WHITE);

            text = body(wb, bodyFont, HorizontalAlignment.LEFT, false);
            textCenter = body(wb, bodyFont, HorizontalAlignment.CENTER, false);
            textWrap = body(wb, bodyFont, HorizontalAlignment.LEFT, true);

            money = body(wb, bodyFont, HorizontalAlignment.RIGHT, false);
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        }

        private CellStyle textFor(int column) {
            if (contains(CENTER_COLS, column)) {
                return textCenter;
            }
            return contains(WRAP_COLS, column) ? textWrap : text;
        }

        private static CellStyle body(XSSFWorkbook wb, Font font, HorizontalAlignment align, boolean wrap) {
            CellStyle style = wb.createCellStyle();
            style.setFont(font);
            style.setAlignment(align);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(wrap);
            border(style, IndexedColors.GREY_25_PERCENT);
            return style;
        }

        private static void border(CellStyle style, IndexedColors color) {
            short code = color.getIndex();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(code);
            style.setBottomBorderColor(code);
            style.setLeftBorderColor(code);
            style.setRightBorderColor(code);
        }

        private static Font font(XSSFWorkbook wb, int size, boolean bold, IndexedColors color) {
            Font font = wb.createFont();
            // 中文字体，避免默认 Calibri 下中文靠回退字体显示
            font.setFontName("微软雅黑");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setColor(color.getIndex());
            return font;
        }
    }
}
