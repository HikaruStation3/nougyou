package com.nougyou.menu.controller;

import com.nougyou.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用的静态菜单。实际项目可改为从数据库读取并按角色过滤。
 */
@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

    @GetMapping("/routes")
    public ApiResponse<List<Map<String, Object>>> routes() {
        Map<String, Object> userMenu = route("user", "system/user/index", "User",
                "用户管理", "el-icon-User", null, null);
        Map<String, Object> aiMenu = route("chat", "ai/chat/index", "AiChat",
                "AI 对话", "el-icon-ChatDotRound", null, null);
        Map<String, Object> fileMenu = route("file", "demo/file/index", "FileDemo",
                "文件上传", "el-icon-Upload", null, null);

        return ApiResponse.ok(List.of(
                route("/system", "Layout", "/system", "系统管理", "el-icon-Setting",
                        "/system/user", List.of(userMenu)),
                route("/ai", "Layout", "/ai", "AI 助手", "el-icon-ChatDotRound",
                        "/ai/chat", List.of(aiMenu)),
                route("/tools", "Layout", "/tools", "工具箱", "el-icon-FolderOpened",
                        "/tools/file", List.of(fileMenu))
        ));
    }

    private Map<String, Object> route(String path, String component, String name, String title,
                                      String icon, String redirect, List<Map<String, Object>> children) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("path", path);
        if (component != null) {
            node.put("component", component);
        }
        if (redirect != null) {
            node.put("redirect", redirect);
        }
        node.put("name", name);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", title);
        meta.put("icon", icon);
        meta.put("hidden", false);
        meta.put("keepAlive", true);
        // 目录节点始终展示，避免单子菜单被折叠
        meta.put("alwaysShow", children != null);
        node.put("meta", meta);

        if (children != null) {
            node.put("children", children);
        } else {
            node.put("children", List.of());
        }
        return node;
    }
}
