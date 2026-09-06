package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.request.ProductCreateRequest;
import com.study.flashsale.dto.request.ProductUpdateRequest;
import com.study.flashsale.service.ProductService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品", description = "商品查询与管理（写操作需管理员）")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "创建商品")
    @LogOperation("创建商品")
    @PreventDuplicateSubmit
    @PostMapping
    public Result<ProductVO> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return Result.success(productService.createProduct(request));
    }

    @Operation(summary = "修改商品")
    @LogOperation("修改商品")
    @PreventDuplicateSubmit
    @PutMapping("/{id}")
    public Result<ProductVO> updateProduct(@PathVariable String id, @Valid @RequestBody ProductUpdateRequest request) {
        return Result.success(productService.updateProduct(IdUtil.parseId(id), request));
    }

    @Operation(summary = "查询商品列表")
    @LogOperation("查询商品列表")
    @GetMapping
    public Result<PageResponse<ProductVO>> listProducts(@Valid PageRequest request) {
        return Result.success(productService.listProducts(request));
    }

    @Operation(summary = "查询商品详情")
    @LogOperation("查询商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable String id) {
        return Result.success(productService.getProductDetail(IdUtil.parseId(id)));
    }
}
