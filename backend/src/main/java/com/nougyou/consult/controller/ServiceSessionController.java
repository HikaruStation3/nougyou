package com.nougyou.consult.controller;

import com.nougyou.common.api.ApiResponse;
import com.nougyou.common.page.PageResult;
import com.nougyou.consult.dto.ConsultTargetVO;
import com.nougyou.consult.dto.ServiceMessageSendRequest;
import com.nougyou.consult.dto.ServiceMessageVO;
import com.nougyou.consult.dto.ServiceSessionCreateRequest;
import com.nougyou.consult.dto.ServiceSessionQuery;
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

import java.util.List;

/**
 * 沟通咨询（买家/农户侧）：API-067 ~ API-071。
 */
@RestController
@RequestMapping({"/api/v1/service", "/api/service"})
@RequiredArgsConstructor
public class ServiceSessionController {

    private final ServiceConsultService consultService;

    /** API-067 创建或获取会话：targetType=1 找平台客服，=2 直接找商家 */
    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('service:create')")
    public ApiResponse<ServiceSessionVO> createOrGet(@Valid @RequestBody ServiceSessionCreateRequest request) {
        return ApiResponse.ok(consultService.createOrGet(SecurityUtils.currentUser().userId(), request));
    }

    /** API-068 我的会话列表 */
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('service:list')")
    public ApiResponse<PageResult<ServiceSessionVO>> mySessions(ServiceSessionQuery query) {
        return ApiResponse.ok(consultService.mySessions(SecurityUtils.currentUser().userId(), query));
    }

    /** API-069 会话消息列表，传 lastId 增量拉取 */
    @GetMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('service:message')")
    public ApiResponse<PageResult<ServiceMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long lastId) {
        return ApiResponse.ok(consultService.messages(
                id, SecurityUtils.currentUser().userId(), lastId, pageNum, pageSize));
    }

    /** API-070 发送消息 */
    @PostMapping("/sessions/{id}/messages")
    @PreAuthorize("hasAuthority('service:send')")
    public ApiResponse<ServiceMessageVO> send(@PathVariable Long id,
                                              @Valid @RequestBody ServiceMessageSendRequest request) {
        return ApiResponse.ok(consultService.send(id, SecurityUtils.currentUser().userId(), request));
    }

    /** API-071 标记已读 */
    @PutMapping("/sessions/{id}/read")
    @PreAuthorize("hasAuthority('service:read')")
    public ApiResponse<Integer> markRead(@PathVariable Long id) {
        return ApiResponse.ok(consultService.markRead(id, SecurityUtils.currentUser().userId()));
    }

    /** 可咨询的农户商家列表（发起咨询时选择对象） */
    @GetMapping("/farmers")
    @PreAuthorize("hasAuthority('service:create')")
    public ApiResponse<List<ConsultTargetVO>> farmers() {
        return ApiResponse.ok(consultService.consultableFarmers());
    }
}
