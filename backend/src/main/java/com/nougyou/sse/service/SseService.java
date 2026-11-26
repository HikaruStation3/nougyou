package com.nougyou.sse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接池：按用户保存连接，支持单播、批量推送与全站广播。
 * <p>
 * 通知发布时推 {@code notice}、撤回时推 {@code notice-revoke}，在线用户无需刷新即可看到。
 */
@Slf4j
@Component
public class SseService {

    /** userId -> 该用户的全部连接（同一账号可开多个标签页） */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        sendToUser(userId, "online-count", onlineCount());
        return emitter;
    }

    /** 推送给单个用户的所有连接 */
    public void sendToUser(Long userId, String event, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            send(userId, emitter, event, data);
        }
    }

    /** 推送给一批用户（指定接收人的通知） */
    public void sendToUsers(Collection<Long> userIds, String event, Object data) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            sendToUser(userId, event, data);
        }
    }

    /** 推送给所有在线用户（全体通知、在线人数） */
    public void broadcast(String event, Object data) {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                send(userId, emitter, event, data);
            }
        });
    }

    public int onlineCount() {
        return (int) emitters.values().stream().filter(list -> !list.isEmpty()).count();
    }

    private void send(Long userId, SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ex) {
            // 浏览器关闭/网络断开时发送会失败，移除这条连接即可
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        }
    }
}
