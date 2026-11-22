package com.nougyou.consult.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.consult.dto.ConsultTargetVO;
import com.nougyou.consult.dto.ServiceAdminQuery;
import com.nougyou.consult.dto.ServiceCloseRequest;
import com.nougyou.consult.dto.ServiceMessageSendRequest;
import com.nougyou.consult.dto.ServiceMessageVO;
import com.nougyou.consult.dto.ServiceSessionCreateRequest;
import com.nougyou.consult.dto.ServiceSessionQuery;
import com.nougyou.consult.dto.ServiceSessionVO;
import com.nougyou.consult.dto.row.SessionLastMessageRow;
import com.nougyou.consult.dto.row.SessionUnreadRow;
import com.nougyou.consult.dto.row.UserBrief;
import com.nougyou.consult.entity.ServiceMessage;
import com.nougyou.consult.entity.ServiceSession;
import com.nougyou.consult.mapper.ConsultProductMapper;
import com.nougyou.consult.mapper.ConsultUserMapper;
import com.nougyou.consult.mapper.ServiceMessageMapper;
import com.nougyou.consult.mapper.ServiceSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 沟通咨询：买家/农户 ↔ 平台客服（FR-009）、买家 ↔ 农户商家（FR-010）、客服响应（FR-032）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceConsultService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_FARMER = "FARMER";
    private static final String ROLE_BUYER = "BUYER";
    private static final String PLATFORM_NAME = "平台客服";
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ServiceSessionMapper sessionMapper;
    private final ServiceMessageMapper messageMapper;
    private final ConsultUserMapper userMapper;
    private final ConsultProductMapper productMapper;

    /* ------------------------------ 会话 ------------------------------ */

    /**
     * API-067 创建或获取会话：同一对用户的未结束会话直接复用，不重复建。
     */
    @Transactional
    public ServiceSessionVO createOrGet(Long userId, ServiceSessionCreateRequest request) {
        String myRole = userMapper.selectRoleCode(userId);
        if (ROLE_ADMIN.equals(myRole)) {
            throw new BusinessException("管理员不发起咨询，请使用客服工作台回复");
        }
        if (!ROLE_BUYER.equals(myRole) && !ROLE_FARMER.equals(myRole)) {
            throw new BusinessException("当前账号角色不支持发起咨询");
        }

        int targetType = request.getTargetType();
        Long targetId = null;
        if (targetType == ServiceSession.TARGET_FARMER) {
            targetId = request.getTargetId();
            if (targetId == null) {
                throw new BusinessException("联系商家时必须指定商家");
            }
            if (targetId.equals(userId)) {
                throw new BusinessException("不能给自己发起咨询");
            }
            if (!ROLE_FARMER.equals(userMapper.selectRoleCode(targetId))) {
                throw new BusinessException("咨询对象不是农户商家");
            }
        }

        Long productId = request.getProductId();
        if (productId != null && productMapper.selectName(productId) == null) {
            throw new BusinessException("咨询的商品不存在");
        }

        var wrapper = Wrappers.<ServiceSession>lambdaQuery()
                .eq(ServiceSession::getUserId, userId)
                .eq(ServiceSession::getTargetType, targetType)
                .ne(ServiceSession::getStatus, ServiceSession.STATUS_CLOSED);
        if (targetType == ServiceSession.TARGET_FARMER) {
            wrapper.eq(ServiceSession::getTargetId, targetId);
        }
        ServiceSession existing = sessionMapper.selectOne(
                wrapper.orderByDesc(ServiceSession::getId).last("limit 1"));
        if (existing != null) {
            // 复用会话时补全商品上下文，方便商家知道这次问的是哪件
            if (existing.getProductId() == null && productId != null) {
                sessionMapper.update(null, Wrappers.<ServiceSession>lambdaUpdate()
                        .eq(ServiceSession::getId, existing.getId())
                        .set(ServiceSession::getProductId, productId));
                existing.setProductId(productId);
            }
            return toVOList(List.of(existing), userId).get(0);
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceSession session = new ServiceSession();
        session.setSessionNo(nextSessionNo());
        session.setUserId(userId);
        session.setUserRole(myRole);
        session.setTargetType(targetType);
        session.setTargetId(targetId);
        session.setProductId(productId);
        // 平台客服会话先进待接入队列；直连商家会话双方可直接对话
        session.setStatus(targetType == ServiceSession.TARGET_PLATFORM
                ? ServiceSession.STATUS_WAITING : ServiceSession.STATUS_ACTIVE);
        session.setStartTime(now);
        // 创建即写入时间，保证新会话排在列表最前
        session.setLastMsgTime(now);
        sessionMapper.insert(session);
        return toVOList(List.of(session), userId).get(0);
    }

    /**
     * API-068 我的会话列表：发起或作为商家被咨询的会话都在内。
     */
    public PageResult<ServiceSessionVO> mySessions(Long userId, ServiceSessionQuery query) {
        var wrapper = Wrappers.<ServiceSession>lambdaQuery()
                .and(w -> w.eq(ServiceSession::getUserId, userId).or().eq(ServiceSession::getTargetId, userId));
        if (query.getStatus() != null) {
            wrapper.eq(ServiceSession::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(ServiceSession::getLastMsgTime).orderByDesc(ServiceSession::getId);

        Page<ServiceSession> page = sessionMapper.selectPage(
                new Page<>(pageNo(query.getPageNum()), pageSize(query.getPageSize())), wrapper);
        return new PageResult<>(toVOList(page.getRecords(), userId), page.getTotal());
    }

    /**
     * API-072 客服工作台列表：全平台会话，待接入优先。
     */
    public PageResult<ServiceSessionVO> adminSessions(Long adminId, ServiceAdminQuery query) {
        var wrapper = Wrappers.<ServiceSession>lambdaQuery();
        if (query.getTargetType() != null) {
            wrapper.eq(ServiceSession::getTargetType, query.getTargetType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ServiceSession::getStatus, query.getStatus());
        }
        if (Boolean.TRUE.equals(query.getMine())) {
            wrapper.eq(ServiceSession::getAdminId, adminId);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            List<Long> userIds = userMapper.selectIdsByNicknameLike(keyword);
            wrapper.and(w -> {
                w.like(ServiceSession::getSessionNo, keyword);
                if (!userIds.isEmpty()) {
                    w.or().in(ServiceSession::getUserId, userIds);
                }
            });
        }
        // 排队中的排最前，其次按最后消息时间
        wrapper.orderByAsc(ServiceSession::getStatus)
                .orderByDesc(ServiceSession::getLastMsgTime)
                .orderByDesc(ServiceSession::getId);

        Page<ServiceSession> page = sessionMapper.selectPage(
                new Page<>(pageNo(query.getPageNum()), pageSize(query.getPageSize())), wrapper);
        return new PageResult<>(toVOList(page.getRecords(), adminId), page.getTotal());
    }

    /* ------------------------------ 消息 ------------------------------ */

    /**
     * API-069 消息列表：传 lastId 时按增量拉取（升序），否则取最近一页（返回仍按时间升序）。
     */
    public PageResult<ServiceMessageVO> messages(Long sessionId, Long userId, Long lastId,
                                                 long pageNum, long pageSize) {
        requireAccess(sessionId, userId);

        var wrapper = Wrappers.<ServiceMessage>lambdaQuery()
                .eq(ServiceMessage::getSessionId, sessionId);
        boolean incremental = lastId != null && lastId > 0;
        if (incremental) {
            wrapper.gt(ServiceMessage::getId, lastId).orderByAsc(ServiceMessage::getId);
        } else {
            // 无 lastId 时取最近 N 条，返回时再翻转成升序，贴合聊天窗口
            wrapper.orderByDesc(ServiceMessage::getId);
        }

        Page<ServiceMessage> page = messageMapper.selectPage(
                new Page<>(incremental ? 1 : pageNo(pageNum), pageSize(pageSize)), wrapper);
        List<ServiceMessage> records = new ArrayList<>(page.getRecords());
        Collections.reverse(records);
        return new PageResult<>(toMessageVOList(records), page.getTotal());
    }

    /**
     * API-070 发送消息。
     */
    @Transactional
    public ServiceMessageVO send(Long sessionId, Long userId, ServiceMessageSendRequest request) {
        ServiceSession session = requireAccess(sessionId, userId);
        if (Objects.equals(session.getStatus(), ServiceSession.STATUS_CLOSED)) {
            throw new BusinessException("会话已结束，无法继续发送消息");
        }
        if (Integer.valueOf(ServiceMessage.TYPE_IMAGE).equals(request.getContentType())
                && !request.getContent().startsWith("/") && !request.getContent().startsWith("http")) {
            throw new BusinessException("图片消息请传上传后的地址");
        }

        String senderRole = resolveSenderRole(session, userId);
        LocalDateTime now = LocalDateTime.now();

        ServiceMessage message = new ServiceMessage();
        message.setSessionId(sessionId);
        message.setSenderId(userId);
        message.setSenderRole(senderRole);
        message.setContentType(request.getContentType());
        message.setContent(request.getContent().trim());
        message.setIsRead(0);
        message.setCreateTime(now);
        messageMapper.insert(message);

        var update = Wrappers.<ServiceSession>lambdaUpdate()
                .eq(ServiceSession::getId, sessionId)
                .set(ServiceSession::getLastMsgTime, now);
        // 客服一旦回复，会话即视为已接入
        if (ROLE_ADMIN.equals(senderRole)
                && Objects.equals(session.getStatus(), ServiceSession.STATUS_WAITING)) {
            update.set(ServiceSession::getStatus, ServiceSession.STATUS_ACTIVE)
                    .set(ServiceSession::getAdminId, userId);
        }
        sessionMapper.update(null, update);

        return toMessageVOList(List.of(message)).get(0);
    }

    /**
     * API-071 标记已读：把对方发来的未读消息置为已读。
     */
    @Transactional
    public int markRead(Long sessionId, Long userId) {
        requireAccess(sessionId, userId);
        return messageMapper.update(null, Wrappers.<ServiceMessage>lambdaUpdate()
                .eq(ServiceMessage::getSessionId, sessionId)
                .ne(ServiceMessage::getSenderId, userId)
                .eq(ServiceMessage::getIsRead, 0)
                .set(ServiceMessage::getIsRead, 1));
    }

    /* ------------------------------ 客服操作 ------------------------------ */

    /**
     * API-073 接入会话：待接入 → 进行中，记录接待客服。
     */
    @Transactional
    public ServiceSessionVO accept(Long sessionId, Long adminId) {
        ServiceSession session = requireSession(sessionId);
        if (!Objects.equals(session.getTargetType(), ServiceSession.TARGET_PLATFORM)) {
            throw new BusinessException("商家直连会话无需客服接入");
        }
        if (Objects.equals(session.getStatus(), ServiceSession.STATUS_CLOSED)) {
            throw new BusinessException("会话已结束");
        }
        if (session.getAdminId() != null && !session.getAdminId().equals(adminId)) {
            throw new BusinessException("该会话已被其他客服接入");
        }
        if (session.getAdminId() == null) {
            sessionMapper.update(null, Wrappers.<ServiceSession>lambdaUpdate()
                    .eq(ServiceSession::getId, sessionId)
                    .set(ServiceSession::getAdminId, adminId)
                    .set(ServiceSession::getStatus, ServiceSession.STATUS_ACTIVE));
            session.setAdminId(adminId);
            session.setStatus(ServiceSession.STATUS_ACTIVE);
        }
        return toVOList(List.of(session), adminId).get(0);
    }

    /**
     * API-074 结束会话：写入结束时间，可选记录处理小结（以客服消息形式留痕）。
     */
    @Transactional
    public ServiceSessionVO close(Long sessionId, Long adminId, ServiceCloseRequest request) {
        ServiceSession session = requireSession(sessionId);
        if (session.getAdminId() != null && !session.getAdminId().equals(adminId)) {
            throw new BusinessException("只有接入的客服可以结束该会话");
        }
        if (Objects.equals(session.getStatus(), ServiceSession.STATUS_CLOSED)) {
            return toVOList(List.of(session), adminId).get(0);
        }

        LocalDateTime now = LocalDateTime.now();
        String summary = request == null ? null : request.getSummary();
        if (StringUtils.hasText(summary)) {
            ServiceMessage message = new ServiceMessage();
            message.setSessionId(sessionId);
            message.setSenderId(adminId);
            message.setSenderRole(ROLE_ADMIN);
            message.setContentType(ServiceMessage.TYPE_TEXT);
            message.setContent("【处理小结】" + summary.trim());
            message.setIsRead(0);
            message.setCreateTime(now);
            messageMapper.insert(message);
        }

        sessionMapper.update(null, Wrappers.<ServiceSession>lambdaUpdate()
                .eq(ServiceSession::getId, sessionId)
                .set(ServiceSession::getStatus, ServiceSession.STATUS_CLOSED)
                .set(ServiceSession::getEndTime, now)
                .set(ServiceSession::getLastMsgTime, now)
                .set(session.getAdminId() == null, ServiceSession::getAdminId, adminId));
        session.setStatus(ServiceSession.STATUS_CLOSED);
        session.setEndTime(now);
        session.setLastMsgTime(now);
        if (session.getAdminId() == null) {
            session.setAdminId(adminId);
        }
        return toVOList(List.of(session), adminId).get(0);
    }

    /**
     * 可咨询的农户商家列表（买家发起咨询时选择对象）。
     */
    public List<ConsultTargetVO> consultableFarmers() {
        return userMapper.selectFarmers().stream()
                .map(item -> new ConsultTargetVO(item.getId(), item.getNickname(), item.getAvatar()))
                .toList();
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    private ServiceSession requireSession(Long sessionId) {
        ServiceSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    /**
     * 会话参与方：发起人、商家对象、接入客服；管理员额外可见全部会话。
     */
    private ServiceSession requireAccess(Long sessionId, Long userId) {
        ServiceSession session = requireSession(sessionId);
        if (isParticipant(session, userId) || ROLE_ADMIN.equals(userMapper.selectRoleCode(userId))) {
            return session;
        }
        throw new BusinessException("无权访问该会话");
    }

    private boolean isParticipant(ServiceSession session, Long userId) {
        return Objects.equals(session.getUserId(), userId)
                || Objects.equals(session.getTargetId(), userId)
                || Objects.equals(session.getAdminId(), userId);
    }

    /**
     * 发送人角色：客服 > 发起人 > 商家对象（DB 里统一存大写）。
     */
    private String resolveSenderRole(ServiceSession session, Long userId) {
        if (Objects.equals(session.getAdminId(), userId)) {
            return ROLE_ADMIN;
        }
        if (Objects.equals(session.getUserId(), userId)) {
            return StringUtils.hasText(session.getUserRole()) ? session.getUserRole() : ROLE_BUYER;
        }
        if (Objects.equals(session.getTargetId(), userId)) {
            return ROLE_FARMER;
        }
        return ROLE_ADMIN.equals(userMapper.selectRoleCode(userId)) ? ROLE_ADMIN : ROLE_BUYER;
    }

    /**
     * 会话编号：SV + yyyyMMdd + 4 位随机序号，唯一键冲突时重试。
     */
    private String nextSessionNo() {
        String date = LocalDate.now().format(NO_DATE);
        for (int i = 0; i < 5; i++) {
            String no = "SV" + date + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
            boolean exists = sessionMapper.exists(Wrappers.<ServiceSession>lambdaQuery()
                    .eq(ServiceSession::getSessionNo, no));
            if (!exists) {
                return no;
            }
        }
        // 极端情况下退化为毫秒时间戳，保证最终能落库
        return "SV" + System.currentTimeMillis();
    }

    /**
     * 批量组装会话 VO：一次查出参与人信息、未读数、最后一条消息，避免 N+1。
     */
    private List<ServiceSessionVO> toVOList(List<ServiceSession> sessions, Long viewerId) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        List<Long> sessionIds = new ArrayList<>(sessions.size());
        for (ServiceSession session : sessions) {
            sessionIds.add(session.getId());
            userIds.add(session.getUserId());
            if (session.getTargetId() != null) {
                userIds.add(session.getTargetId());
            }
            if (session.getAdminId() != null) {
                userIds.add(session.getAdminId());
            }
            if (session.getProductId() != null) {
                productIds.add(session.getProductId());
            }
        }
        userIds.remove(null);

        Map<Long, UserBrief> users = userMapper.selectBriefs(userIds).stream()
                .collect(Collectors.toMap(UserBrief::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> products = new HashMap<>();
        for (Long productId : productIds) {
            products.put(productId, productMapper.selectName(productId));
        }
        Map<Long, Long> unread = messageMapper.countUnread(sessionIds, viewerId).stream()
                .collect(Collectors.toMap(SessionUnreadRow::getSessionId,
                        row -> row.getCnt() == null ? 0L : row.getCnt(), (a, b) -> a));
        Map<Long, SessionLastMessageRow> lastMessages = messageMapper.selectLastMessages(sessionIds).stream()
                .collect(Collectors.toMap(SessionLastMessageRow::getSessionId,
                        Function.identity(), (a, b) -> a));

        List<ServiceSessionVO> result = new ArrayList<>(sessions.size());
        for (ServiceSession session : sessions) {
            UserBrief initiator = users.get(session.getUserId());
            UserBrief target = session.getTargetId() == null ? null : users.get(session.getTargetId());
            UserBrief admin = session.getAdminId() == null ? null : users.get(session.getAdminId());
            String initiatorName = nickname(initiator);
            String targetName = Objects.equals(session.getTargetType(), ServiceSession.TARGET_PLATFORM)
                    ? PLATFORM_NAME : nickname(target);

            SessionLastMessageRow last = lastMessages.get(session.getId());
            result.add(ServiceSessionVO.builder()
                    .id(session.getId())
                    .sessionNo(session.getSessionNo())
                    .userId(session.getUserId())
                    .userNickname(initiatorName)
                    .userRole(lower(session.getUserRole()))
                    .targetType(session.getTargetType())
                    .targetId(session.getTargetId())
                    .targetName(targetName)
                    .adminId(session.getAdminId())
                    .adminName(admin == null ? null : nickname(admin))
                    .status(session.getStatus())
                    .title(titleOf(session, viewerId, initiatorName, targetName))
                    .lastMsgTime(session.getLastMsgTime())
                    .unreadCount(unread.getOrDefault(session.getId(), 0L).intValue())
                    .productId(session.getProductId())
                    .productName(session.getProductId() == null ? null : products.get(session.getProductId()))
                    .lastMsgPreview(preview(last))
                    .lastMsgType(last == null ? null : last.getContentType())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .build());
        }
        return result;
    }

    private List<ServiceMessageVO> toMessageVOList(List<ServiceMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> senderIds = messages.stream().map(ServiceMessage::getSenderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserBrief> users = userMapper.selectBriefs(senderIds).stream()
                .collect(Collectors.toMap(UserBrief::getId, Function.identity(), (a, b) -> a));

        List<ServiceMessageVO> result = new ArrayList<>(messages.size());
        for (ServiceMessage message : messages) {
            UserBrief sender = users.get(message.getSenderId());
            result.add(ServiceMessageVO.builder()
                    .id(message.getId())
                    .sessionId(message.getSessionId())
                    .senderId(message.getSenderId())
                    .senderRole(lower(message.getSenderRole()))
                    .senderName(nickname(sender))
                    .senderAvatar(sender == null ? null : sender.getAvatar())
                    .contentType(message.getContentType())
                    .content(message.getContent())
                    .isRead(message.getIsRead())
                    .createTime(message.getCreateTime())
                    .build());
        }
        return result;
    }

    /**
     * 会话标题按查看者视角取对方名称。
     */
    private String titleOf(ServiceSession session, Long viewerId, String initiatorName, String targetName) {
        if (viewerId != null && viewerId.equals(session.getAdminId())) {
            return initiatorName + " · " + targetName;
        }
        if (viewerId != null && viewerId.equals(session.getTargetId())) {
            return initiatorName;
        }
        return targetName;
    }

    private String preview(SessionLastMessageRow last) {
        if (last == null || !StringUtils.hasText(last.getContent())) {
            return null;
        }
        if (Integer.valueOf(ServiceMessage.TYPE_IMAGE).equals(last.getContentType())) {
            return "[图片]";
        }
        String text = last.getContent().replaceAll("\\s+", " ").trim();
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }

    private static String nickname(UserBrief brief) {
        if (brief == null) {
            return "未知用户";
        }
        return StringUtils.hasText(brief.getNickname()) ? brief.getNickname() : "用户" + brief.getId();
    }

    private static String lower(String role) {
        return role == null ? null : role.toLowerCase();
    }

    private static long pageNo(long pageNum) {
        return pageNum < 1 ? 1 : pageNum;
    }

    private static long pageSize(long pageSize) {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
