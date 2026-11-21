package com.nougyou.sse.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在线人数 SSE 演示：前端 useSse 默认连接 /api/v1/sse/connect。
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/connect")
    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("online-count").data(emitters.size()));
        } catch (IOException ex) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @Scheduled(fixedRate = 30_000)
    public void broadcastOnlineCount() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("online-count").data(emitters.size()));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }
}
