package com.nougyou.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nougyou.notice.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
