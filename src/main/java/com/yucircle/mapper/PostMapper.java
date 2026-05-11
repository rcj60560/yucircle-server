package com.yucircle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yucircle.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
