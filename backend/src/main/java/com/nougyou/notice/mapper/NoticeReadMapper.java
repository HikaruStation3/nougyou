package com.nougyou.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nougyou.notice.entity.NoticeRead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeReadMapper extends BaseMapper<NoticeRead> {

    /**
     * 一键全部已读：把该用户所有可见的已发布通知批量写入已读表。
     * INSERT IGNORE 依赖 uk_notice_user 唯一键，已读过的自动跳过。
     */
    @Insert("""
            INSERT IGNORE INTO notice_read (notice_id, user_id)
            SELECT n.id, #{userId}
              FROM notice n
             WHERE n.deleted = 0
               AND n.publish_status = 1
               AND (n.target_type = 1
                    OR EXISTS (SELECT 1 FROM notice_target t
                                WHERE t.notice_id = n.id AND t.user_id = #{userId}))
            """)
    int insertAllRead(@Param("userId") Long userId);
}
