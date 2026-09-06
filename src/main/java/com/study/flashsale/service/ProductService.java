package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.request.ProductCreateRequest;
import com.study.flashsale.dto.request.ProductUpdateRequest;
import com.study.flashsale.entity.Product;
import com.study.flashsale.vo.ProductVO;

public interface ProductService extends IService<Product> {

    ProductVO createProduct(ProductCreateRequest request);

    ProductVO updateProduct(Long id, ProductUpdateRequest request);

    PageResponse<ProductVO> listProducts(PageRequest request);

    ProductVO getProductDetail(Long id);
}
