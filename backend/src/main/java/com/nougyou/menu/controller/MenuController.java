package com.nougyou.menu.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.security.SecurityUtils;
import com.nougyou.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 演示用的静态菜单，按当前用户的权限点过滤。实际项目可改为从数据库读取。
 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final PermissionService permissionService;

    @GetMapping("/routes")
    public ApiResponse<List<Map<String, Object>>> routes() {
        Set<String> perms = new HashSet<>(
                permissionService.findPermissionCodes(SecurityUtils.currentUser().userId()));

        List<Map<String, Object>> routes = new ArrayList<>();
        List<Map<String, Object>> systemMenus = new ArrayList<>();
        if (perms.contains("sys:user:menu")) {
            systemMenus.add(route("user", "system/user/index", "User",
                    "用户管理", "el-icon-User", null, null));
        }
        if (perms.contains("sys:role:menu")) {
            systemMenus.add(route("role", "system/role/index", "Role",
                    "角色权限", "el-icon-Lock", null, null));
        }
        if (perms.contains("farmerApply:list")) {
            systemMenus.add(route("farmer-apply", "system/farmer-apply/index", "FarmerApply",
                    "入驻审核", "el-icon-DocumentChecked", null, null));
        }
        if (!systemMenus.isEmpty()) {
            String redirect = String.valueOf(systemMenus.get(0).get("path"));
            routes.add(route("/system", "Layout", "/system", "系统管理", "el-icon-Setting",
                    "/system/" + redirect, systemMenus));
        }
        if (perms.contains("ai:chat:menu")) {
            Map<String, Object> aiMenu = route("chat", "ai/chat/index", "AiChat",
                    "AI 对话", "el-icon-ChatDotRound", null, null);
            routes.add(route("/ai", "Layout", "/ai", "AI 助手", "el-icon-ChatDotRound",
                    "/ai/chat", List.of(aiMenu)));
        }
        if (perms.contains("file:upload:menu")) {
            Map<String, Object> fileMenu = route("file", "demo/file/index", "FileDemo",
                    "文件上传", "el-icon-Upload", null, null);
            routes.add(route("/tools", "Layout", "/tools", "工具箱", "el-icon-FolderOpened",
                    "/tools/file", List.of(fileMenu)));
        }
        return ApiResponse.ok(routes);
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
