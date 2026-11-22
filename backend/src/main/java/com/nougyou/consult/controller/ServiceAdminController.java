package com.nougyou.consult.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.consult.dto.ServiceAdminQuery;
import com.nougyou.consult.dto.ServiceCloseRequest;
import com.nougyou.consult.dto.ServiceMessageSendRequest;
import com.nougyou.consult.dto.ServiceMessageVO;
import com.nougyou.consult.dto.ServiceSessionVO;
import com.nougyou.consult.service.ServiceConsultService;
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

/**
 * 在线客服工作台（管理员侧）：API-072 ~ API-074。
 */
@RestController
@RequestMapping({"/api/v1/admin/service", "/api/admin/service"})
@RequiredArgsConstructor
public class ServiceAdminController {

    private final ServiceConsultService consultService;

    /** API-072 客服待接入列表（全部会话，待接入优先） */
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('service:adminList')")
    public ApiResponse<PageResult<ServiceSessionVO>> sessions(ServiceAdminQuery query) {
        return ApiResponse.ok(consultService.adminSessions(SecurityUtils.currentUser().userId(), query));
    }

    /** 会话消息全文（客服查看） */
    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('service:message')")
    public ApiResponse<PageResult<ServiceMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "50") long pageSize,
            @RequestParam(required = false) Long lastId) {
        return ApiResponse.ok(consultService.messages(
                id, SecurityUtils.currentUser().userId(), lastId, pageNum, pageSize));
    }

    /** 客服回复消息 */
    @PostMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('service:send')")
    public ApiResponse<ServiceMessageVO> send(@PathVariable Long id,
                                              @Valid @RequestBody ServiceMessageSendRequest request) {
        return ApiResponse.ok(consultService.send(id, SecurityUtils.currentUser().userId(), request));
    }

    /** 客服查看会话后标记已读 */
    @PutMapping("/sessions/{id}/read")
    @PreAuthorize("hasAuthority('service:read')")
    public ApiResponse<Integer> markRead(@PathVariable Long id) {
        return ApiResponse.ok(consultService.markRead(id, SecurityUtils.currentUser().userId()));
    }

    /** API-073 客服接入会话 */
    @PutMapping("/sessions/{id}/accept")
    @PreAuthorize("hasAuthority('service:accept')")
    public ApiResponse<ServiceSessionVO> accept(@PathVariable Long id) {
        return ApiResponse.ok(consultService.accept(id, SecurityUtils.currentUser().userId()));
    }

    /** API-074 客服结束会话（可附处理小结） */
    @PutMapping("/sessions/{id}/close")
    @PreAuthorize("hasAuthority('service:close')")
    public ApiResponse<ServiceSessionVO> close(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) ServiceCloseRequest request) {
        return ApiResponse.ok(consultService.close(id, SecurityUtils.currentUser().userId(), request));
    }
}
