package com.nougyou.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.api.ResultCode;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.notice.dto.NoticeDetailVO;
import com.nougyou.notice.dto.NoticeForm;
import com.nougyou.notice.dto.NoticePageQuery;
import com.nougyou.notice.dto.NoticeVO;
import com.nougyou.notice.entity.Notice;
import com.nougyou.notice.entity.NoticeRead;
import com.nougyou.notice.entity.NoticeTarget;
import com.nougyou.notice.mapper.NoticeMapper;
import com.nougyou.notice.mapper.NoticeReadMapper;
import com.nougyou.notice.mapper.NoticeTargetMapper;
import com.nougyou.sse.service.SseService;
import com.nougyou.system.entity.SysUser;
import com.nougyou.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知公告：管理员发布，全站或指定用户接收。
 * <p>
 * 可见性口径：只有 publish_status = 1 的通知对用户可见；target_type = 2 时还要在
 * notice_target 中命中当前用户。「已读」以 notice_read 有无记录判定，没有记录即未读。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int REVOKED = -1;
    private static final int TARGET_ALL = 1;
    private static final int TARGET_SPECIFIED = 2;
    private static final long MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NoticeMapper noticeMapper;
    private final NoticeTargetMapper targetMapper;
    private final NoticeReadMapper readMapper;
    private final SysUserMapper userMapper;
    private final SseService sseService;

    // ==================== 管理端 ====================

    /** 管理端分页：包含草稿与已撤回，按创建时间倒序 */
    public PageResult<NoticeVO> adminPage(NoticePageQuery query) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = Wrappers.<Notice>lambdaQuery();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Notice::getTitle, query.getTitle().trim());
        }
        if (query.getPublishStatus() != null) {
            wrapper.eq(Notice::getPublishStatus, query.getPublishStatus());
        }
        wrapper.orderByDesc(Notice::getCreateTime).orderByDesc(Notice::getId);

        Page<Notice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> publisherNames = publisherNames(page.getRecords());
        List<NoticeVO> items = page.getRecords().stream()
                .map(notice -> toVO(notice, publisherNames.get(notice.getPublisherId()), null))
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    /** 编辑回显 */
    public NoticeForm form(Long id) {
        Notice notice = require(id);
        NoticeForm form = new NoticeForm();
        form.setId(notice.getId());
        form.setTitle(notice.getTitle());
        form.setContent(notice.getContent());
        form.setType(notice.getType());
        form.setLevel(notice.getLevel());
        form.setTargetType(notice.getTargetType());
        form.setTargetUserIds(targetUserIds(id));
        return form;
    }

    @Transactional
    public Long create(NoticeForm form, Long publisherId) {
        Notice notice = new Notice();
        applyForm(notice, form);
        notice.setPublishStatus(DRAFT);
        notice.setPublisherId(publisherId);
        noticeMapper.insert(notice);
        saveTargets(notice.getId(), notice.getTargetType(), form.getTargetUserIds());
        return notice.getId();
    }

    @Transactional
    public void update(Long id, NoticeForm form) {
        Notice notice = require(id);
        if (Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
            throw new BusinessException("通知已发布，需先撤回才能编辑");
        }
        applyForm(notice, form);
        noticeMapper.updateById(notice);

        targetMapper.delete(Wrappers.<NoticeTarget>lambdaQuery().eq(NoticeTarget::getNoticeId, id));
        saveTargets(id, notice.getTargetType(), form.getTargetUserIds());
    }

    /** 批量删除，逗号分隔；已发布的通知必须先撤回 */
    @Transactional
    public void delete(String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            throw new BusinessException("请选择要删除的通知");
        }
        for (Long id : idList) {
            Notice notice = noticeMapper.selectById(id);
            if (notice == null) {
                continue;
            }
            if (Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
                throw new BusinessException("通知「" + notice.getTitle() + "」已发布，请先撤回再删除");
            }
        }
        noticeMapper.deleteByIds(idList);
        targetMapper.delete(Wrappers.<NoticeTarget>lambdaQuery().in(NoticeTarget::getNoticeId, idList));
        readMapper.delete(Wrappers.<NoticeRead>lambdaQuery().in(NoticeRead::getNoticeId, idList));
    }

    /** 发布：置为已发布并推送 SSE，指定用户的通知只推给目标用户 */
    @Transactional
    public void publish(Long id, Long operatorId) {
        Notice notice = require(id);
        if (Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
            throw new BusinessException("该通知已发布");
        }
        LocalDateTime now = LocalDateTime.now();
        noticeMapper.update(null, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getId, id)
                .set(Notice::getPublishStatus, PUBLISHED)
                .set(Notice::getPublishTime, now)
                .set(Notice::getRevokeTime, null)
                .set(notice.getPublisherId() == null, Notice::getPublisherId, operatorId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("title", notice.getTitle());
        payload.put("type", notice.getType());
        payload.put("level", notice.getLevel());
        payload.put("publishTime", now.format(FMT));
        push(notice, "notice", payload);
    }

    /** 撤回：置为已撤回并通知在线的接收人移除该条 */
    @Transactional
    public void revoke(Long id) {
        Notice notice = require(id);
        if (!Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
            throw new BusinessException("只有已发布的通知可以撤回");
        }
        noticeMapper.update(null, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getId, id)
                .set(Notice::getPublishStatus, REVOKED)
                .set(Notice::getRevokeTime, LocalDateTime.now()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        push(notice, "notice-revoke", payload);
    }

    // ==================== 我的通知 ====================

    /** 当前用户可见的通知分页：已发布 + （全体 或 指定给我） */
    public PageResult<NoticeVO> myPage(NoticePageQuery query, Long userId) {
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), MAX_PAGE_SIZE);

        var wrapper = visibleWrapper(userId);
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Notice::getTitle, query.getTitle().trim());
        }
        if (query.getIsRead() != null) {
            String readSub = "select notice_id from notice_read where user_id = " + userId;
            if (query.getIsRead() == 1) {
                wrapper.inSql(Notice::getId, readSub);
            } else {
                wrapper.notInSql(Notice::getId, readSub);
            }
        }
        wrapper.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);

        Page<Notice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> publisherNames = publisherNames(page.getRecords());
        Set<Long> readIds = readIdsOf(userId, page.getRecords());
        List<NoticeVO> items = page.getRecords().stream()
                .map(notice -> toVO(notice, publisherNames.get(notice.getPublisherId()),
                        readIds.contains(notice.getId()) ? 1 : 0))
                .toList();
        return new PageResult<>(items, page.getTotal());
    }

    /**
     * 通知详情：管理员可看任意状态；普通用户只能看发给自己的已发布通知。
     * 无论谁查看都记一条已读——管理员也会在顶部铃铛收到全体通知，点开就该消掉未读数。
     */
    @Transactional
    public NoticeDetailVO detail(Long id, Long userId, boolean admin) {
        Notice notice = require(id);
        if (!admin) {
            if (!Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
                throw new BusinessException("通知不存在或已撤回");
            }
            if (Integer.valueOf(TARGET_SPECIFIED).equals(notice.getTargetType()) && !isTarget(id, userId)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权查看该通知");
            }
        }
        if (Integer.valueOf(PUBLISHED).equals(notice.getPublishStatus())) {
            markRead(id, userId);
        }

        List<Long> targets = Integer.valueOf(TARGET_SPECIFIED).equals(notice.getTargetType())
                ? targetUserIds(id) : List.of();
        return NoticeDetailVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .type(notice.getType())
                .level(notice.getLevel())
                .publishStatus(notice.getPublishStatus())
                .publisherName(publisherName(notice.getPublisherId()))
                .targetUserIds(targets.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .publishTime(notice.getPublishTime())
                .createTime(notice.getCreateTime())
                .build();
    }

    /** 一键全部已读 */
    @Transactional
    public void readAll(Long userId) {
        readMapper.insertAllRead(userId);
    }

    // ==================== 内部方法 ====================

    private LambdaQueryWrapper<Notice> visibleWrapper(Long userId) {
        return Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getPublishStatus, PUBLISHED)
                .and(w -> w.eq(Notice::getTargetType, TARGET_ALL)
                        .or().inSql(Notice::getId,
                                "select notice_id from notice_target where user_id = " + userId));
    }

    private void push(Notice notice, String event, Map<String, Object> payload) {
        if (Integer.valueOf(TARGET_SPECIFIED).equals(notice.getTargetType())) {
            sseService.sendToUsers(targetUserIds(notice.getId()), event, payload);
        } else {
            sseService.broadcast(event, payload);
        }
    }

    private void applyForm(Notice notice, NoticeForm form) {
        notice.setTitle(form.getTitle().trim());
        notice.setContent(form.getContent());
        notice.setType(form.getType() == null ? 1 : form.getType());
        notice.setLevel(StringUtils.hasText(form.getLevel()) ? form.getLevel() : "L");
        notice.setTargetType(form.getTargetType() == null ? TARGET_ALL : form.getTargetType());
    }

    private void saveTargets(Long noticeId, Integer targetType, List<Long> userIds) {
        if (!Integer.valueOf(TARGET_SPECIFIED).equals(targetType) || userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : new HashSet<>(userIds)) {
            NoticeTarget target = new NoticeTarget();
            target.setNoticeId(noticeId);
            target.setUserId(userId);
            targetMapper.insert(target);
        }
    }

    private List<Long> targetUserIds(Long noticeId) {
        return targetMapper.selectList(Wrappers.<NoticeTarget>lambdaQuery()
                        .eq(NoticeTarget::getNoticeId, noticeId))
                .stream().map(NoticeTarget::getUserId).toList();
    }

    private boolean isTarget(Long noticeId, Long userId) {
        return targetMapper.selectCount(Wrappers.<NoticeTarget>lambdaQuery()
                .eq(NoticeTarget::getNoticeId, noticeId)
                .eq(NoticeTarget::getUserId, userId)) > 0;
    }

    private Set<Long> readIdsOf(Long userId, List<Notice> notices) {
        if (notices.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = notices.stream().map(Notice::getId).toList();
        return readMapper.selectList(Wrappers.<NoticeRead>lambdaQuery()
                        .eq(NoticeRead::getUserId, userId)
                        .in(NoticeRead::getNoticeId, ids))
                .stream().map(NoticeRead::getNoticeId).collect(Collectors.toSet());
    }

    private void markRead(Long noticeId, Long userId) {
        if (readMapper.selectCount(Wrappers.<NoticeRead>lambdaQuery()
                .eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId)) > 0) {
            return;
        }
        NoticeRead record = new NoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        try {
            readMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            // 并发重复点击详情时唯一键冲突，已读状态一致，忽略即可
        }
    }

    private Map<Long, String> publisherNames(List<Notice> notices) {
        Set<Long> ids = notices.stream().map(Notice::getPublisherId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, ids))
                .stream().collect(Collectors.toMap(SysUser::getId,
                        user -> StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername()));
    }

    private String publisherName(Long publisherId) {
        if (publisherId == null) {
            return "系统";
        }
        SysUser user = userMapper.selectById(publisherId);
        if (user == null) {
            return "系统";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private NoticeVO toVO(Notice notice, String publisherName, Integer isRead) {
        return NoticeVO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .type(notice.getType())
                .level(notice.getLevel())
                .targetType(notice.getTargetType())
                .publishStatus(notice.getPublishStatus())
                .isRead(isRead)
                .publisherName(publisherName == null ? "系统" : publisherName)
                .publishTime(notice.getPublishTime())
                .revokeTime(notice.getRevokeTime())
                .createTime(notice.getCreateTime())
                .build();
    }

    private Notice require(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("通知不存在");
        }
        return notice;
    }

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.valueOf(value));
            } catch (NumberFormatException ex) {
                throw new BusinessException("通知ID格式不正确：" + value);
            }
        }
        return result;
    }
}
