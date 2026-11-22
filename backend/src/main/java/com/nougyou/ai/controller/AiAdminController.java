package com.nougyou.ai.controller;

import com.nougyou.ai.dto.AiOverviewVO;
import com.nougyou.ai.dto.ChatMessageVO;
import com.nougyou.ai.dto.ChatSessionVO;
import com.nougyou.ai.dto.PromptConfigVO;
import com.nougyou.ai.dto.PromptSaveRequest;
import com.nougyou.ai.service.AiAdminService;
import com.nougyou.ai.service.AiPromptAdminService;
import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员 AI 管理接口：对话监控（FR-036）与提示词版本管理（FR-037）。
 */
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiAdminService adminService;
    private final AiPromptAdminService promptAdminService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('ai:adminSession')")
    public ApiResponse<AiOverviewVO> overview() {
        return ApiResponse.ok(adminService.overview());
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('ai:adminSession')")
    public ApiResponse<PageResult<ChatSessionVO>> sessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminService.pageSessions(
                userId, sceneCode, startTime, endTime, keyword, pageNum, pageSize));
    }

    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('ai:adminMessage')")
    public ApiResponse<PageResult<ChatMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminService.listMessages(id, pageNum, pageSize));
    }

    @GetMapping("/prompts")
    @PreAuthorize("hasAuthority('ai:promptList')")
    public ApiResponse<List<PromptConfigVO>> prompts(@RequestParam(required = false) String sceneCode) {
        return ApiResponse.ok(promptAdminService.list(sceneCode));
    }

    @PostMapping("/prompts")
    @PreAuthorize("hasAuthority('ai:promptAdd')")
    public ApiResponse<PromptConfigVO> createPrompt(@Valid @RequestBody PromptSaveRequest request) {
        return ApiResponse.ok(promptAdminService.create(request, SecurityUtils.currentUser().userId()));
    }

    @PutMapping("/prompts/{id}/enable")
    @PreAuthorize("hasAuthority('ai:promptEnable')")
    public ApiResponse<PromptConfigVO> enablePrompt(@PathVariable Long id) {
        return ApiResponse.ok(promptAdminService.enable(id, SecurityUtils.currentUser().userId()));
    }
}
