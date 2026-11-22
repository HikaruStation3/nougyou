package com.nougyou.consult.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nougyou.consult.dto.row.SessionLastMessageRow;
import com.nougyou.consult.dto.row.SessionUnreadRow;
import com.nougyou.consult.entity.ServiceMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ServiceMessageMapper extends BaseMapper<ServiceMessage> {

    /**
     * 批量统计「别人发给我且未读」的消息数，用于会话列表红点。
     */
    @Select("<script>" +
            "select session_id as sessionId, count(*) as cnt from service_message " +
            "where sender_id &lt;&gt; #{userId} and is_read = 0 and session_id in " +
            "<foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "group by session_id" +
            "</script>")
    List<SessionUnreadRow> countUnread(@Param("sessionIds") Collection<Long> sessionIds,
                                       @Param("userId") Long userId);

    /**
     * 批量取每个会话的最后一条消息，用于列表预览。
     */
    @Select("<script>" +
            "select session_id as sessionId, content as content, content_type as contentType " +
            "from service_message where id in (" +
            "  select max(id) from service_message where session_id in " +
            "  <foreach collection='sessionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  group by session_id)" +
            "</script>")
    List<SessionLastMessageRow> selectLastMessages(@Param("sessionIds") Collection<Long> sessionIds);
}
