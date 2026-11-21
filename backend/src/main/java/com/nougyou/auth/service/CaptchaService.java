package com.nougyou.auth.service;

import com.nougyou.auth.dto.CaptchaResponse;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码：内存存储，5 分钟过期。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final long TTL_SECONDS = 300;

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${nougyou.captcha.enabled:true}")
    private boolean enabled;

    public CaptchaResponse generate() {
        String code = randomCode();
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new CaptchaEntry(code, Instant.now().plusSeconds(TTL_SECONDS).toEpochMilli()));
        return new CaptchaResponse(id, "data:image/png;base64," + render(code));
    }

    public void validate(String captchaId, String captchaCode) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "请输入验证码");
        }
        CaptchaEntry entry = store.remove(captchaId);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()
                || !entry.code().equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR, "验证码错误或已过期");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 248, 255));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < 6; i++) {
                graphics.setColor(randomColor(180, 220));
                graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(randomColor(30, 120));
                graphics.drawString(String.valueOf(code.charAt(i)), 14 + i * 25, 29 + random.nextInt(4));
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException("验证码生成失败");
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }

    private record CaptchaEntry(String code, long expireAt) {
    }
}
