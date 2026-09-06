package com.study.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.flashsale.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}