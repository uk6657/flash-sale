package com.study.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.flashsale.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}