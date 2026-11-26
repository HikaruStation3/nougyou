package com.nougyou.sse.controller;

import com.nougyou.security.SecurityUtils;
import com.nougyou.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接入口：前端 useSse 连接 /api/v1/sse/connect。
 * <p>
 * 连接按登录用户分组，通知公告等业务可按用户精准推送；online-count 为在线人数心跳。
 */
@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping("/connect")
    public SseEmitter connect() {
        return sseService.connect(SecurityUtils.currentUser().userId());
    }

    @Scheduled(fixedRate = 30_000)
    public void broadcastOnlineCount() {
        sseService.broadcast("online-count", sseService.onlineCount());
    }
}
