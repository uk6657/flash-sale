package com.study.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.study.flashsale.common.ErrorCode;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.RedisKeyConstants;
import com.study.flashsale.config.FlashSaleProperties;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.dto.request.ProductCreateRequest;
import com.study.flashsale.dto.request.ProductUpdateRequest;
import com.study.flashsale.entity.Product;
import com.study.flashsale.enums.CommonStatus;
import com.study.flashsale.exception.BusinessException;
import com.study.flashsale.mapper.ProductMapper;
import com.study.flashsale.infrastructure.cache.CacheService;
import com.study.flashsale.service.ProductService;
import com.study.flashsale.infrastructure.redis.RedisService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final RedisService redisService;

    private final CacheService cacheService;

    private final FlashSaleProperties flashSaleProperties;

    @Override
    public ProductVO createProduct(ProductCreateRequest request) {
        if (request.getStatus() != null && !CommonStatus.isValid(request.getStatus())) {
            throw new BusinessException("商品状态只能是0或1");
        }

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getName, request.getName());
        Product existProduct = getOne(queryWrapper);
        if (existProduct != null) {
            throw new BusinessException("商品名称已存在");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription() == null ? "" : request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setLockStock(0);
        product.setStatus(request.getStatus() == null ? CommonStatus.ENABLED.getCode() : request.getStatus());

        save(product);
        return toProductVO(product);
    }

    @Override
    public ProductVO updateProduct(Long id, ProductUpdateRequest request) {
        Product oldProduct = getById(id);
        if (oldProduct == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        if (!CommonStatus.isValid(request.getStatus())) {
            throw new BusinessException("商品状态只能是0或1");
        }

        Integer lockStock = oldProduct.getLockStock() == null ? 0 : oldProduct.getLockStock();
        if (request.getStock() < lockStock) {
            throw new BusinessException("商品总库存不能小于已冻结库存");
        }

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getName, request.getName());
        queryWrapper.ne(Product::getId, id);
        Product sameNameProduct = getOne(queryWrapper);
        if (sameNameProduct != null) {
            throw new BusinessException("商品名称已存在");
        }

        Product product = new Product();
        product.setId(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription() == null ? "" : request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setLockStock(lockStock);
        product.setStatus(request.getStatus());

        updateById(product);
        redisService.delete(RedisKeyConstants.productDetail(id));

        return toProductVO(getById(id));
    }

    @Override
    public PageResponse<ProductVO> listProducts(PageRequest request) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, 1);
        queryWrapper.orderByDesc(Product::getId);

        Page<Product> page = page(new Page<>(request.getCurrent(), request.getSize()), queryWrapper);

        List<ProductVO> records = page.getRecords().stream()
                .map(this::toProductVO)
                .toList();

        return new PageResponse<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                records
        );
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        return cacheService.queryWithPassThroughAndMutex(
                RedisKeyConstants.productDetail(id),
                RedisKeyConstants.lockProductDetail(id),
                ProductVO.class,
                () -> getProductDetailFromDb(id),
                Duration.ofMinutes(flashSaleProperties.getCache().getProductDetailMinutes()),
                "商品不存在或已下架"
        );
    }

    private ProductVO getProductDetailFromDb(Long id) {
        Product product = getById(id);
        if (product == null || CommonStatus.DISABLED.getCode().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }
        return toProductVO(product);
    }

    private ProductVO toProductVO(Product product) {
        Integer lockStock = product.getLockStock() == null ? 0 : product.getLockStock();
        return new ProductVO(
                IdUtil.toString(product.getId()),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                lockStock,
                product.getStock() - lockStock,
                product.getStatus(),
                CommonStatus.getTextByCode(product.getStatus())
        );
    }


}

