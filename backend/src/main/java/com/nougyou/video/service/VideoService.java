package com.nougyou.video.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nougyou.common.exception.BusinessException;
import com.nougyou.common.page.PageResult;
import com.nougyou.product.dto.ProductVO;
import com.nougyou.product.entity.Product;
import com.nougyou.product.mapper.ProductMapper;
import com.nougyou.security.SecurityUtils;
import com.nougyou.video.dto.AdminVideoQuery;
import com.nougyou.video.dto.FarmerProfileVO;
import com.nougyou.video.dto.MyVideoQuery;
import com.nougyou.video.dto.VideoAuditForm;
import com.nougyou.video.dto.VideoCommentForm;
import com.nougyou.video.dto.VideoCommentVO;
import com.nougyou.video.dto.VideoPageQuery;
import com.nougyou.video.dto.VideoPublishForm;
import com.nougyou.video.dto.VideoVO;
import com.nougyou.video.dto.row.VideoUserBrief;
import com.nougyou.video.entity.Video;
import com.nougyou.video.entity.VideoComment;
import com.nougyou.video.entity.VideoFollow;
import com.nougyou.video.entity.VideoLike;
import com.nougyou.video.mapper.VideoCommentMapper;
import com.nougyou.video.mapper.VideoFollowMapper;
import com.nougyou.video.mapper.VideoLikeMapper;
import com.nougyou.video.mapper.VideoMapper;
import com.nougyou.video.mapper.VideoReferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 短视频：观看信息流（FR-011）、点赞 / 评论 / 关注（FR-012）、
 * 商家发布与审核进度（FR-020 / FR-021）、管理员审核（FR-035）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REPLY_DEPTH = 8;
    private static final int PROFILE_PREVIEW_SIZE = 10;
    private static final String ROLE_FARMER = "FARMER";
    private static final String SORT_HOT = "hot";

    private final VideoMapper videoMapper;
    private final VideoLikeMapper likeMapper;
    private final VideoCommentMapper commentMapper;
    private final VideoFollowMapper followMapper;
    private final VideoReferenceMapper referenceMapper;
    private final ProductMapper productMapper;

    /* ============================================================
     * 一、观看：列表 / 详情 / 播放量（API-075 / 076）
     * ============================================================ */

    /**
     * API-075 短视频信息流：只返回审核通过且未下架的视频，游客可匿名浏览。
     */
    public PageResult<VideoVO> publicPage(VideoPageQuery query, Long viewerId) {
        var wrapper = Wrappers.<Video>lambdaQuery()
                .eq(Video::getAuditStatus, Video.AUDIT_PASS)
                .eq(Video::getStatus, Video.STATUS_ON);
        if (query.getFarmerId() != null) {
            wrapper.eq(Video::getFarmerId, query.getFarmerId());
        }
        if (SORT_HOT.equalsIgnoreCase(query.getSortBy())) {
            wrapper.orderByDesc(Video::getLikeCount).orderByDesc(Video::getId);
        } else {
            wrapper.orderByDesc(Video::getPublishTime).orderByDesc(Video::getId);
        }
        Page<Video> page = videoMapper.selectPage(
                new Page<>(pageNo(query.getPageNum()), pageSize(query.getPageSize())), wrapper);
        return new PageResult<>(toVOList(page.getRecords(), viewerId), page.getTotal());
    }

    /**
     * API-076 播放页数据：含当前用户的点赞与关注状态。
     */
    public VideoVO detail(Long id, Long viewerId) {
        Video video = requireVideo(id);
        requireVisible(video, viewerId);
        return toVOList(List.of(video), viewerId).get(0);
    }

    /**
     * 播放量 +1：播放页每次真正开始播放时调用，幂等性由前端按会话去重。
     */
    @Transactional
    public void recordView(Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null || !isPublic(video)) {
            return;
        }
        videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                .eq(Video::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    /* ============================================================
     * 二、互动：点赞 / 评论 / 关注（API-080 ~ 084）
     * ============================================================ */

    /**
     * API-080 点赞 / 取消点赞：幂等接口，已点赞则取消，未点赞则点赞。
     */
    @Transactional
    public VideoVO toggleLike(Long videoId, Long userId) {
        Video video = requireVideo(videoId);
        requireVisible(video, userId);

        boolean liked = likeMapper.exists(Wrappers.<VideoLike>lambdaQuery()
                .eq(VideoLike::getVideoId, videoId)
                .eq(VideoLike::getUserId, userId));
        if (liked) {
            likeMapper.delete(Wrappers.<VideoLike>lambdaQuery()
                    .eq(VideoLike::getVideoId, videoId)
                    .eq(VideoLike::getUserId, userId));
            videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                    .eq(Video::getId, videoId)
                    .setSql("like_count = IF(like_count > 0, like_count - 1, 0)"));
        } else {
            VideoLike like = new VideoLike();
            like.setVideoId(videoId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            try {
                likeMapper.insert(like);
                videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                        .eq(Video::getId, videoId)
                        .setSql("like_count = like_count + 1"));
            } catch (DuplicateKeyException ex) {
                // 并发下同一用户重复点赞：唯一键已保证只计一次，直接按已点赞返回
                log.debug("重复点赞已忽略 videoId={} userId={}", videoId, userId);
            }
        }
        Video latest = videoMapper.selectById(videoId);
        return toVOList(List.of(latest), userId).get(0);
    }

    /**
     * API-081 评论列表：一级评论分页，回复归并到所属一级评论下。
     */
    public PageResult<VideoCommentVO> comments(Long videoId, long pageNum, long pageSize, Long viewerId) {
        Video video = requireVideo(videoId);
        Page<VideoComment> page = commentMapper.selectPage(
                new Page<>(pageNo(pageNum), Math.min(pageSize(pageSize), 50)),
                Wrappers.<VideoComment>lambdaQuery()
                        .eq(VideoComment::getVideoId, videoId)
                        .eq(VideoComment::getParentId, VideoComment.ROOT_PARENT_ID)
                        .eq(VideoComment::getStatus, VideoComment.STATUS_NORMAL)
                        .orderByDesc(VideoComment::getCreateTime)
                        .orderByDesc(VideoComment::getId));
        List<VideoComment> tops = page.getRecords();
        if (tops.isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal());
        }

        // 一次性取出该视频的全部回复，在内存里按「根评论」归并成两级结构
        List<VideoComment> replies = commentMapper.selectList(Wrappers.<VideoComment>lambdaQuery()
                .eq(VideoComment::getVideoId, videoId)
                .ne(VideoComment::getParentId, VideoComment.ROOT_PARENT_ID)
                .eq(VideoComment::getStatus, VideoComment.STATUS_NORMAL)
                .orderByAsc(VideoComment::getCreateTime)
                .orderByAsc(VideoComment::getId));

        Map<Long, VideoComment> byId = new HashMap<>();
        tops.forEach(comment -> byId.put(comment.getId(), comment));
        replies.forEach(comment -> byId.put(comment.getId(), comment));

        Map<Long, List<VideoComment>> childrenMap = new HashMap<>();
        for (VideoComment reply : replies) {
            VideoComment root = rootOf(reply, byId);
            if (root == null || !byId.containsKey(root.getId()) || !tops.contains(root)) {
                continue;
            }
            if (root.getId().equals(reply.getId())) {
                continue;
            }
            childrenMap.computeIfAbsent(root.getId(), key -> new ArrayList<>()).add(reply);
        }

        Map<Long, VideoUserBrief> users = briefs(allUserIds(tops, replies));
        Set<Long> deletable = new HashSet<>();
        boolean admin = SecurityUtils.hasAuthority("video:audit");
        List<VideoCommentVO> result = new ArrayList<>(tops.size());
        for (VideoComment top : tops) {
            result.add(toCommentVO(top, byId, childrenMap, users, video, viewerId, admin));
            if (canDelete(top, video, viewerId, admin)) {
                deletable.add(top.getId());
            }
        }
        return new PageResult<>(result, page.getTotal());
    }

    /**
     * API-082 发表评论 / 回复。
     */
    @Transactional
    public VideoCommentVO addComment(Long videoId, Long userId, VideoCommentForm form) {
        Video video = requireVideo(videoId);
        if (!isPublic(video)) {
            throw new BusinessException("视频未通过审核或已下架，暂不能评论");
        }
        String content = form.getContent() == null ? "" : form.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 200) {
            throw new BusinessException("评论内容最长 200 字");
        }

        long parentId = form.getParentId() == null ? 0L : form.getParentId();
        VideoComment parent = null;
        if (parentId > 0) {
            parent = commentMapper.selectById(parentId);
            if (parent == null
                    || !Objects.equals(parent.getVideoId(), videoId)
                    || !Objects.equals(parent.getStatus(), VideoComment.STATUS_NORMAL)) {
                throw new BusinessException("被回复的评论不存在或已删除");
            }
        }

        VideoComment comment = new VideoComment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus(VideoComment.STATUS_NORMAL);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                .eq(Video::getId, videoId)
                .setSql("comment_count = comment_count + 1"));

        Map<Long, VideoUserBrief> users = briefs(List.of(userId, video.getFarmerId()));
        VideoCommentVO vo = toCommentVO(comment, Map.of(comment.getId(), comment), Map.of(),
                users, video, userId, false);
        if (parent != null) {
            VideoUserBrief parentUser = users.get(parent.getUserId());
            vo.setReplyToName(parentUser == null ? null : nickname(parentUser));
        }
        vo.setCanDelete(true);
        vo.setMine(true);
        return vo;
    }

    /**
     * API-083 删除评论：作者本人、视频所属商家、管理员可删；
     * 删除一级评论时一并删除其下回复，评论数按实际删除条数回退。
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        VideoComment comment = commentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getStatus(), VideoComment.STATUS_NORMAL)) {
            throw new BusinessException("评论不存在或已删除");
        }
        Video video = requireVideo(comment.getVideoId());
        boolean admin = SecurityUtils.hasAuthority("video:audit");
        if (!canDelete(comment, video, userId, admin)) {
            throw new BusinessException("只能删除自己发表的评论");
        }

        Set<Long> ids = new LinkedHashSet<>();
        ids.add(commentId);
        if (Objects.equals(comment.getParentId(), VideoComment.ROOT_PARENT_ID)) {
            ids.addAll(descendantIds(comment.getVideoId(), commentId));
        }
        commentMapper.update(null, Wrappers.<VideoComment>lambdaUpdate()
                .in(VideoComment::getId, ids)
                .set(VideoComment::getStatus, VideoComment.STATUS_DELETED));
        videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                .eq(Video::getId, video.getId())
                .setSql("comment_count = GREATEST(comment_count - " + ids.size() + ", 0)"));
    }

    /**
     * API-084 关注 / 取消关注商家：幂等接口。
     */
    @Transactional
    public FarmerProfileVO toggleFollow(Long farmerId, Long userId) {
        if (Objects.equals(farmerId, userId)) {
            throw new BusinessException("不能关注自己");
        }
        if (!ROLE_FARMER.equals(referenceMapper.selectRoleCode(farmerId))) {
            throw new BusinessException("只能关注农户商家");
        }
        boolean followed = followMapper.exists(Wrappers.<VideoFollow>lambdaQuery()
                .eq(VideoFollow::getUserId, userId)
                .eq(VideoFollow::getFarmerId, farmerId));
        if (followed) {
            followMapper.delete(Wrappers.<VideoFollow>lambdaQuery()
                    .eq(VideoFollow::getUserId, userId)
                    .eq(VideoFollow::getFarmerId, farmerId));
        } else {
            VideoFollow follow = new VideoFollow();
            follow.setUserId(userId);
            follow.setFarmerId(farmerId);
            follow.setCreateTime(LocalDateTime.now());
            try {
                followMapper.insert(follow);
            } catch (DuplicateKeyException ex) {
                log.debug("重复关注已忽略 farmerId={} userId={}", farmerId, userId);
            }
        }
        return farmerProfile(farmerId, userId);
    }

    /**
     * API-085 商家主页：资料 + 在售商品前 10 条 + 已发布视频前 10 条。
     */
    public FarmerProfileVO farmerProfile(Long farmerId, Long viewerId) {
        VideoUserBrief brief = referenceMapper.selectBrief(farmerId);
        if (brief == null || !ROLE_FARMER.equals(brief.getRoleCode())) {
            throw new BusinessException("商家不存在");
        }
        boolean followed = viewerId != null && followMapper.exists(Wrappers.<VideoFollow>lambdaQuery()
                .eq(VideoFollow::getUserId, viewerId)
                .eq(VideoFollow::getFarmerId, farmerId));
        long followCount = followMapper.selectCount(Wrappers.<VideoFollow>lambdaQuery()
                .eq(VideoFollow::getFarmerId, farmerId));

        var productWrapper = Wrappers.<Product>lambdaQuery()
                .eq(Product::getFarmerId, farmerId)
                .eq(Product::getShelfStatus, 1)
                .orderByDesc(Product::getId);
        long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getFarmerId, farmerId)
                .eq(Product::getShelfStatus, 1));
        List<Product> products = productMapper.selectList(
                productWrapper.last("limit " + PROFILE_PREVIEW_SIZE));

        var videoWrapper = Wrappers.<Video>lambdaQuery()
                .eq(Video::getFarmerId, farmerId)
                .eq(Video::getAuditStatus, Video.AUDIT_PASS)
                .eq(Video::getStatus, Video.STATUS_ON)
                .orderByDesc(Video::getPublishTime)
                .orderByDesc(Video::getId);
        long videoCount = videoMapper.selectCount(Wrappers.<Video>lambdaQuery()
                .eq(Video::getFarmerId, farmerId)
                .eq(Video::getAuditStatus, Video.AUDIT_PASS)
                .eq(Video::getStatus, Video.STATUS_ON));
        List<Video> videos = videoMapper.selectList(videoWrapper.last("limit " + PROFILE_PREVIEW_SIZE));

        return FarmerProfileVO.builder()
                .farmerId(farmerId)
                .realName(nickname(brief))
                .shopName(brief.getShopName())
                .avatar(brief.getAvatar())
                .village(null)
                .isFollowed(followed)
                .followCount((int) followCount)
                .productCount((int) productCount)
                .videoCount((int) videoCount)
                .products(products.stream().map(this::toProductVO).toList())
                .videos(toVOList(videos, viewerId))
                .build();
    }

    /* ============================================================
     * 三、商家自助：发布 / 我的短视频 / 删除（API-077 ~ 079）
     * ============================================================ */

    /**
     * API-077 发布短视频：发布后进入待审核，审核通过才对外展示。
     */
    @Transactional
    public VideoVO publish(Long farmerId, VideoPublishForm form) {
        if (!ROLE_FARMER.equals(referenceMapper.selectRoleCode(farmerId))) {
            throw new BusinessException("只有农户商家可以发布短视频");
        }
        if (form.getDuration() == null || form.getDuration() < 1 || form.getDuration() > 600) {
            throw new BusinessException("视频时长需在 1-600 秒之间");
        }
        requireUploadedUrl(form.getVideoUrl(), "视频");
        requireUploadedUrl(form.getCover(), "封面");

        Video video = new Video();
        video.setFarmerId(farmerId);
        video.setTitle(form.getTitle().trim());
        video.setCover(form.getCover().trim());
        video.setVideoUrl(form.getVideoUrl().trim());
        video.setDuration(form.getDuration());
        video.setDescription(StringUtils.hasText(form.getDescription())
                ? form.getDescription().trim() : null);
        video.setAuditStatus(Video.AUDIT_PENDING);
        video.setLikeCount(0);
        video.setCommentCount(0);
        video.setViewCount(0);
        video.setStatus(Video.STATUS_ON);
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());
        videoMapper.insert(video);
        return toVOList(List.of(video), farmerId).get(0);
    }

    /**
     * API-078 我的短视频：包含待审核与驳回记录，便于商家查看进度与驳回原因。
     */
    public PageResult<VideoVO> myVideos(Long farmerId, MyVideoQuery query) {
        var wrapper = Wrappers.<Video>lambdaQuery().eq(Video::getFarmerId, farmerId);
        if (query.getAuditStatus() != null) {
            wrapper.eq(Video::getAuditStatus, query.getAuditStatus());
        }
        wrapper.orderByDesc(Video::getCreateTime).orderByDesc(Video::getId);
        Page<Video> page = videoMapper.selectPage(
                new Page<>(pageNo(query.getPageNum()), pageSize(query.getPageSize())), wrapper);
        return new PageResult<>(toVOList(page.getRecords(), farmerId), page.getTotal());
    }

    /**
     * API-079 删除短视频：仅限本人发布，逻辑删除。
     */
    @Transactional
    public void deleteVideo(Long videoId, Long farmerId) {
        Video video = requireVideo(videoId);
        if (!Objects.equals(video.getFarmerId(), farmerId)) {
            throw new BusinessException("只能删除自己发布的短视频");
        }
        videoMapper.deleteById(videoId);
    }

    /* ============================================================
     * 四、管理端：列表与审核（API-086）
     * ============================================================ */

    /**
     * 管理端短视频列表：待审核优先，支持审核状态与关键字筛选。
     */
    public PageResult<VideoVO> adminPage(AdminVideoQuery query) {
        var wrapper = Wrappers.<Video>lambdaQuery();
        if (query.getAuditStatus() != null) {
            wrapper.eq(Video::getAuditStatus, query.getAuditStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            List<Long> farmerIds = referenceMapper.selectUserIdsByNickname(keyword);
            wrapper.and(item -> {
                item.like(Video::getTitle, keyword);
                if (!farmerIds.isEmpty()) {
                    item.or().in(Video::getFarmerId, farmerIds);
                }
            });
        }
        wrapper.orderByAsc(Video::getAuditStatus)
                .orderByDesc(Video::getCreateTime)
                .orderByDesc(Video::getId);
        Page<Video> page = videoMapper.selectPage(
                new Page<>(pageNo(query.getPageNum()), pageSize(query.getPageSize())), wrapper);
        return new PageResult<>(toVOList(page.getRecords(), null), page.getTotal());
    }

    /**
     * API-086 审核短视频：通过后写入展示时间并上架，驳回则下架并保留驳回原因。
     */
    @Transactional
    public VideoVO audit(Long videoId, Long adminId, VideoAuditForm form) {
        Video video = requireVideo(videoId);
        int auditStatus = form.getAuditStatus() == null ? -1 : form.getAuditStatus();
        if (auditStatus != Video.AUDIT_PASS && auditStatus != Video.AUDIT_REJECT) {
            throw new BusinessException("审核结果只能是 1 通过或 2 驳回");
        }
        String remark = StringUtils.hasText(form.getAuditRemark()) ? form.getAuditRemark().trim() : null;
        if (auditStatus == Video.AUDIT_REJECT && remark == null) {
            throw new BusinessException("驳回时请填写审核意见");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean pass = auditStatus == Video.AUDIT_PASS;
        videoMapper.update(null, Wrappers.<Video>lambdaUpdate()
                .eq(Video::getId, videoId)
                .set(Video::getAuditStatus, auditStatus)
                .set(Video::getAuditUserId, adminId)
                .set(Video::getAuditTime, now)
                .set(Video::getAuditRemark, remark)
                .set(Video::getStatus, pass ? Video.STATUS_ON : Video.STATUS_OFF)
                .set(pass && video.getPublishTime() == null, Video::getPublishTime, now)
                .set(!pass, Video::getPublishTime, null));
        Video latest = videoMapper.selectById(videoId);
        return toVOList(List.of(latest), adminId).get(0);
    }

    /* ============================================================
     * 内部方法
     * ============================================================ */

    private Video requireVideo(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("短视频不存在或已删除");
        }
        return video;
    }

    private boolean isPublic(Video video) {
        return Objects.equals(video.getAuditStatus(), Video.AUDIT_PASS)
                && Objects.equals(video.getStatus(), Video.STATUS_ON);
    }

    /**
     * 未通过审核的视频仅作者本人与管理员可见，避免待审核内容提前外泄。
     */
    private void requireVisible(Video video, Long viewerId) {
        if (isPublic(video)) {
            return;
        }
        boolean owner = viewerId != null && Objects.equals(video.getFarmerId(), viewerId);
        if (!owner && !SecurityUtils.hasAuthority("video:audit")) {
            throw new BusinessException("视频不存在或未通过审核");
        }
    }

    private void requireUploadedUrl(String url, String label) {
        if (!StringUtils.hasText(url)) {
            throw new BusinessException("请先上传" + label);
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("/") && !trimmed.startsWith("http")) {
            throw new BusinessException(label + "地址不合法，请重新上传");
        }
    }

    private boolean canDelete(VideoComment comment, Video video, Long viewerId, boolean admin) {
        if (viewerId == null) {
            return false;
        }
        return admin
                || Objects.equals(comment.getUserId(), viewerId)
                || Objects.equals(video.getFarmerId(), viewerId);
    }

    /**
     * 找到评论的一级祖先；数据异常（环）时返回 null，避免死循环。
     */
    private VideoComment rootOf(VideoComment comment, Map<Long, VideoComment> byId) {
        VideoComment current = comment;
        for (int depth = 0; depth < MAX_REPLY_DEPTH; depth++) {
            if (Objects.equals(current.getParentId(), VideoComment.ROOT_PARENT_ID)) {
                return current;
            }
            VideoComment parent = byId.get(current.getParentId());
            if (parent == null) {
                return null;
            }
            current = parent;
        }
        return null;
    }

    /**
     * 一级评论下的全部后代（用于级联删除）。
     */
    private Set<Long> descendantIds(Long videoId, Long rootId) {
        List<VideoComment> all = commentMapper.selectList(Wrappers.<VideoComment>lambdaQuery()
                .eq(VideoComment::getVideoId, videoId)
                .ne(VideoComment::getParentId, VideoComment.ROOT_PARENT_ID)
                .eq(VideoComment::getStatus, VideoComment.STATUS_NORMAL));
        Set<Long> result = new HashSet<>();
        Set<Long> frontier = new HashSet<>();
        frontier.add(rootId);
        while (!frontier.isEmpty()) {
            Set<Long> next = new HashSet<>();
            for (VideoComment comment : all) {
                if (frontier.contains(comment.getParentId()) && result.add(comment.getId())) {
                    next.add(comment.getId());
                }
            }
            frontier = next;
        }
        return result;
    }

    private VideoCommentVO toCommentVO(VideoComment comment, Map<Long, VideoComment> byId,
                                       Map<Long, List<VideoComment>> childrenMap,
                                       Map<Long, VideoUserBrief> users, Video video,
                                       Long viewerId, boolean admin) {
        List<VideoComment> replies = childrenMap.getOrDefault(comment.getId(), List.of());
        List<VideoCommentVO> children = new ArrayList<>(replies.size());
        for (VideoComment reply : replies) {
            VideoUserBrief author = users.get(reply.getUserId());
            VideoComment parent = byId.get(reply.getParentId());
            VideoUserBrief replyTo = parent == null ? null : users.get(parent.getUserId());
            children.add(VideoCommentVO.builder()
                    .id(reply.getId())
                    .videoId(reply.getVideoId())
                    .userId(reply.getUserId())
                    .nickname(nickname(author))
                    .avatar(author == null ? null : author.getAvatar())
                    .parentId(reply.getParentId())
                    .replyToName(replyTo == null ? null : nickname(replyTo))
                    .content(reply.getContent())
                    .likeCount(reply.getLikeCount())
                    .createTime(reply.getCreateTime())
                    .children(List.of())
                    .canDelete(canDelete(reply, video, viewerId, admin))
                    .mine(viewerId != null && Objects.equals(reply.getUserId(), viewerId))
                    .build());
        }
        VideoUserBrief author = users.get(comment.getUserId());
        return VideoCommentVO.builder()
                .id(comment.getId())
                .videoId(comment.getVideoId())
                .userId(comment.getUserId())
                .nickname(nickname(author))
                .avatar(author == null ? null : author.getAvatar())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .createTime(comment.getCreateTime())
                .children(children)
                .canDelete(canDelete(comment, video, viewerId, admin))
                .mine(viewerId != null && Objects.equals(comment.getUserId(), viewerId))
                .build();
    }

    /**
     * 批量组装视频 VO：一次查出商家资料、当前用户的点赞与关注集合，避免 N+1。
     */
    private List<VideoVO> toVOList(List<Video> videos, Long viewerId) {
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }
        List<Long> videoIds = videos.stream().map(Video::getId).filter(Objects::nonNull).distinct().toList();
        List<Long> farmerIds = videos.stream().map(Video::getFarmerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, VideoUserBrief> farmers = briefs(farmerIds);

        Set<Long> likedIds = new HashSet<>();
        if (viewerId != null && !videoIds.isEmpty()) {
            likedIds.addAll(likeMapper.selectList(Wrappers.<VideoLike>lambdaQuery()
                            .eq(VideoLike::getUserId, viewerId)
                            .in(VideoLike::getVideoId, videoIds))
                    .stream().map(VideoLike::getVideoId).toList());
        }
        Set<Long> followedIds = new HashSet<>();
        if (viewerId != null && !farmerIds.isEmpty()) {
            followedIds.addAll(followMapper.selectList(Wrappers.<VideoFollow>lambdaQuery()
                            .eq(VideoFollow::getUserId, viewerId)
                            .in(VideoFollow::getFarmerId, farmerIds))
                    .stream().map(VideoFollow::getFarmerId).toList());
        }

        List<VideoVO> result = new ArrayList<>(videos.size());
        for (Video video : videos) {
            VideoUserBrief farmer = farmers.get(video.getFarmerId());
            result.add(VideoVO.builder()
                    .id(video.getId())
                    .farmerId(video.getFarmerId())
                    .farmerName(nickname(farmer))
                    .shopName(farmer == null ? null : farmer.getShopName())
                    .farmerAvatar(farmer == null ? null : farmer.getAvatar())
                    .title(video.getTitle())
                    .cover(video.getCover())
                    .videoUrl(video.getVideoUrl())
                    .duration(video.getDuration())
                    .description(video.getDescription())
                    .auditStatus(video.getAuditStatus())
                    .auditRemark(video.getAuditRemark())
                    .likeCount(safe(video.getLikeCount()))
                    .commentCount(safe(video.getCommentCount()))
                    .viewCount(safe(video.getViewCount()))
                    .liked(likedIds.contains(video.getId()))
                    .followed(followedIds.contains(video.getFarmerId()))
                    .mine(viewerId != null && Objects.equals(video.getFarmerId(), viewerId))
                    .status(video.getStatus())
                    .publishTime(video.getPublishTime())
                    .createTime(video.getCreateTime())
                    .build());
        }
        return result;
    }

    private Map<Long, VideoUserBrief> briefs(List<Long> userIds) {
        List<Long> ids = userIds == null ? List.of()
                : userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return referenceMapper.selectBriefs(ids).stream()
                .collect(Collectors.toMap(VideoUserBrief::getId, Function.identity(), (a, b) -> a));
    }

    private List<Long> allUserIds(List<VideoComment> tops, List<VideoComment> replies) {
        List<Long> ids = new ArrayList<>();
        tops.forEach(comment -> ids.add(comment.getUserId()));
        replies.forEach(comment -> ids.add(comment.getUserId()));
        return ids;
    }

    private ProductVO toProductVO(Product product) {
        return ProductVO.builder()
                .id(String.valueOf(product.getId()))
                .farmerId(product.getFarmerId() == null ? null : String.valueOf(product.getFarmerId()))
                .name(product.getName())
                .cover(product.getCover())
                .price(product.getPrice())
                .stock(product.getStock())
                .unit(product.getUnit())
                .saleCount(product.getSaleCount())
                .shelfStatus(product.getShelfStatus())
                .build();
    }

    private static String nickname(VideoUserBrief brief) {
        if (brief == null) {
            return "钟山镇农户";
        }
        return StringUtils.hasText(brief.getNickname()) ? brief.getNickname() : "用户" + brief.getId();
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
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
