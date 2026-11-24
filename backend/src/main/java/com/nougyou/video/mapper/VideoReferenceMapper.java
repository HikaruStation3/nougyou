package com.nougyou.video.mapper;

import com.nougyou.video.dto.row.VideoUserBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 短视频模块的只读关联查询：用户角色与店铺名来自权限体系，
 * 在这里单独查询，避免与用户 / 商家模块产生循环依赖。
 */
@Mapper
public interface VideoReferenceMapper {

    @Select("<script>" +
            "select u.id, u.nickname, u.avatar, d.name as shopName, r.code as roleCode " +
            "from sys_user u " +
            "left join sys_dept d on d.id = u.dept_id " +
            "left join sys_role r on r.id = u.role_id " +
            "where u.deleted = 0 and u.id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<VideoUserBrief> selectBriefs(@Param("ids") Collection<Long> ids);

    @Select("select u.id, u.nickname, u.avatar, d.name as shopName, r.code as roleCode " +
            "from sys_user u " +
            "left join sys_dept d on d.id = u.dept_id " +
            "left join sys_role r on r.id = u.role_id " +
            "where u.deleted = 0 and u.id = #{userId}")
    VideoUserBrief selectBrief(@Param("userId") Long userId);

    @Select("select r.code from sys_user u left join sys_role r on r.id = u.role_id " +
            "where u.deleted = 0 and u.id = #{userId}")
    String selectRoleCode(@Param("userId") Long userId);

    /**
     * 按昵称模糊匹配用户 ID，供管理端按商家名称搜索视频。
     */
    @Select("select id from sys_user where deleted = 0 and nickname like concat('%', #{keyword}, '%')")
    List<Long> selectUserIdsByNickname(@Param("keyword") String keyword);
}
