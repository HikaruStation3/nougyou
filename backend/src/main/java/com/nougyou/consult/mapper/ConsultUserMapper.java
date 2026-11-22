package com.nougyou.consult.mapper;

import com.nougyou.consult.dto.row.UserBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 会话展示所需的用户信息只读查询，避免与用户模块耦合。
 */
@Mapper
public interface ConsultUserMapper {

    @Select("<script>" +
            "select u.id, u.nickname, u.avatar, r.code as roleCode " +
            "from sys_user u left join sys_role r on r.id = u.role_id " +
            "where u.deleted = 0 and u.id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<UserBrief> selectBriefs(@Param("ids") Collection<Long> ids);

    @Select("select r.code from sys_user u left join sys_role r on r.id = u.role_id " +
            "where u.id = #{userId} and u.deleted = 0")
    String selectRoleCode(@Param("userId") Long userId);

    /**
     * 可咨询的农户商家列表（买家选择咨询对象用）。
     */
    @Select("select u.id, u.nickname, u.avatar, r.code as roleCode " +
            "from sys_user u join sys_role r on r.id = u.role_id " +
            "where u.deleted = 0 and u.status = 1 and r.code = 'FARMER' " +
            "order by u.id")
    List<UserBrief> selectFarmers();

    /**
     * 按昵称模糊匹配用户 ID，供客服工作台关键字搜索会话。
     */
    @Select("select id from sys_user where deleted = 0 and nickname like concat('%', #{keyword}, '%')")
    List<Long> selectIdsByNicknameLike(@Param("keyword") String keyword);
}
