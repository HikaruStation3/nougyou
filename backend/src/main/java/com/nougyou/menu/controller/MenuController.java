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
        if (perms.contains("project:audit")) {
            systemMenus.add(route("project", "system/project/index", "Project",
                    "农家乐审核", "el-icon-Document", null, null));
        }
        if (perms.contains("product:manage")) {
            systemMenus.add(route("product", "system/product/index", "Product",
                    "农产品管理", "el-icon-ShoppingCart", null, null));
            systemMenus.add(route("category", "system/category/index", "ProductCategory",
                    "农产品分类", "el-icon-Grid", null, null));
        }
        if (perms.contains("order:adminList")) {
            systemMenus.add(route("order", "system/order/index", "OrderQuery",
                    "订单查询", "el-icon-List", null, null));
        }
        if (perms.contains("entrust:settle")) {
            systemMenus.add(route("entrust", "system/entrust/index", "EntrustSettle",
                    "委托结算", "el-icon-Money", null, null));
        }
        if (perms.contains("ai:adminSession")) {
            systemMenus.add(route("ai-session", "ai/admin/session/index", "AiSessionMonitor",
                    "AI 对话监控", "el-icon-ChatLineSquare", null, null));
        }
        if (perms.contains("ai:promptList")) {
            systemMenus.add(route("ai-prompt", "ai/admin/prompt/index", "AiPromptConfig",
                    "AI 提示词管理", "el-icon-EditPen", null, null));
        }
        if (perms.contains("service:adminList")) {
            systemMenus.add(route("consult", "consult/admin/index", "ServiceWorkbench",
                    "客服工作台", "el-icon-Service", null, null));
        }
        if (perms.contains("video:audit")) {
            systemMenus.add(route("video", "system/video/index", "VideoAudit",
                    "短视频审核", "el-icon-VideoCamera", null, null));
        }
        if (perms.contains("sys:notice:menu")) {
            systemMenus.add(route("notice", "system/notice/index", "Notice",
                    "通知公告", "el-icon-Bell", null, null));
        }
        if (!systemMenus.isEmpty()) {
            String redirect = String.valueOf(systemMenus.get(0).get("path"));
            routes.add(route("/system", "Layout", "/system", "系统管理", "el-icon-Setting",
                    "/system/" + redirect, systemMenus));
        }
        // 农户商家侧：AI 对话 + 销量统计
        List<Map<String, Object>> aiMenus = new ArrayList<>();
        if (perms.contains("ai:chat:menu")) {
            aiMenus.add(route("chat", "ai/chat/index", "AiChat",
                    "AI 对话", "el-icon-ChatDotRound", null, null));
        }
        if (perms.contains("ai:salesStat")) {
            aiMenus.add(route("stat", "ai/stat/index", "AiSalesStat",
                    "销量统计", "el-icon-DataAnalysis", null, null));
        }
        if (!aiMenus.isEmpty()) {
            String redirect = String.valueOf(aiMenus.get(0).get("path"));
            routes.add(route("/ai", "Layout", "/ai", "AI 助手", "el-icon-ChatDotRound",
                    "/ai/" + redirect, aiMenus));
        }
        if (perms.contains("file:upload:menu")) {
            Map<String, Object> fileMenu = route("file", "demo/file/index", "FileDemo",
                    "文件上传", "el-icon-Upload", null, null);
            routes.add(route("/tools", "Layout", "/tools", "工具箱", "el-icon-FolderOpened",
                    "/tools/file", List.of(fileMenu)));
        }
        // 买家/农户商家：沟通咨询（FR-009 / FR-010）；管理员走客服工作台
        List<Map<String, Object>> consultMenus = new ArrayList<>();
        if (perms.contains("service:list") && !perms.contains("service:adminList")) {
            consultMenus.add(route("chat", "consult/index", "ServiceConsult",
                    "我的咨询", "el-icon-ChatLineRound", null, null));
        }
        if (!consultMenus.isEmpty()) {
            String redirect = String.valueOf(consultMenus.get(0).get("path"));
            routes.add(route("/consult", "Layout", "/consult", "沟通咨询", "el-icon-ChatLineRound",
                    "/consult/" + redirect, consultMenus));
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
