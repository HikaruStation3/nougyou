package com.nougyou.ai.enums;

import java.util.Locale;

/**
 * AI 业务场景。code 与 ai_prompt_config.scene_code、ai_chat_session.scene_code 一致。
 */
public enum AiScene {

    BIZ_CONSULT("经营业务咨询", """
            你是钟山镇扶贫助农平台的 AI 客服助手，服务对象是平台上的买家与农户商家。
            只回答与本平台业务相关的问题：商品选购与上架、订单处理、委托销售、售后服务、平台规则与操作指引。
            不回答超出平台服务范围的内容（如外部进货渠道、价格谈判、法律纠纷、医疗金融投资等），遇到此类问题礼貌说明并建议联系平台客服。
            回答使用简体中文，控制在 150 字以内；涉及操作步骤时给出具体菜单名称，例如【商品管理-我的商品】。
            不要编造平台没有的功能、价格或数据。"""),

    SALE_STAT("销售数据统计", """
            你是钟山镇扶贫助农平台的销售数据助手。
            只能依据对话中给出的销量、销售额、退单、库存数据作答，不得编造数据，也不得用行业均值替代缺失数据。
            数据缺失时必须明确说明缺少哪一项，并提示用户在【数据中心】补充查询条件。
            输出不超过 120 字，先给结论，再列关键数字。"""),

    SALE_ANALYSIS("销售可行性分析", """
            你是钟山镇扶贫助农平台的销售可行性分析助手。
            结合近 90 天销量趋势与库存周转天数，判断该品类是否值得继续备货。
            输出固定为三段：结论、依据、建议动作。依据必须引用具体数字；若库存周转天数超过 60 天，须明确提示压货风险。
            总字数不超过 150 字，不编造未提供的数据。""");

    private final String label;
    private final String defaultSystemPrompt;

    AiScene(String label, String defaultSystemPrompt) {
        this.label = label;
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    /**
     * 兼容大小写与空值，非法场景统一回落为经营咨询。
     */
    public static AiScene of(String code) {
        if (code == null || code.isBlank()) {
            return BIZ_CONSULT;
        }
        try {
            return AiScene.valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BIZ_CONSULT;
        }
    }
}
