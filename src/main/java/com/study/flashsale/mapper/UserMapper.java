package com.study.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.flashsale.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}