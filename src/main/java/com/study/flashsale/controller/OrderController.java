package com.study.flashsale.controller;

import com.study.flashsale.annotation.LogOperation;
import com.study.flashsale.annotation.PreventDuplicateSubmit;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.common.Result;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.service.OrderService;
import com.study.flashsale.util.IdUtil;
import com.study.flashsale.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单", description = "我的订单查询、支付与取消")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "查询我的订单")
    @LogOperation("查询我的订单")
    @GetMapping
    public Result<PageResponse<OrderVO>> listMyOrders(@Valid PageRequest request) {
        return Result.success(orderService.listMyOrders(request));
    }

    @Operation(summary = "查询订单详情")
    @LogOperation("查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable String id) {
        return Result.success(orderService.getOrderDetail(IdUtil.parseId(id)));
    }

    @Operation(summary = "模拟支付订单")
    @LogOperation("模拟支付订单")
    @PreventDuplicateSubmit
    @PostMapping("/{id}/pay")
    public Result<OrderVO> payOrder(@PathVariable String id) {
        return Result.success(orderService.payOrder(IdUtil.parseId(id)));
    }

    @Operation(summary = "取消订单")
    @LogOperation("取消订单")
    @PreventDuplicateSubmit
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable String id) {
        return Result.success(orderService.cancelOrder(IdUtil.parseId(id)));
    }

    @Operation(summary = "模拟支付失败")
    @LogOperation("模拟支付失败")
    @PreventDuplicateSubmit
    @PostMapping("/{id}/pay-fail")
    public Result<OrderVO> failPayOrder(@PathVariable String id) {
        return Result.success(orderService.failPayOrder(IdUtil.parseId(id)));
    }
}
