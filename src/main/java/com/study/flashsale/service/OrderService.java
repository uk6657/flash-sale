package com.study.flashsale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.study.flashsale.common.PageResponse;
import com.study.flashsale.dto.request.PageRequest;
import com.study.flashsale.entity.Order;
import com.study.flashsale.vo.OrderVO;

public interface OrderService extends IService<Order> {

    Long createFlashSaleOrder(Long userId, Long activityId);

    PageResponse<OrderVO> listMyOrders(PageRequest request);

    PageResponse<OrderVO> listAllOrders(PageRequest request);

    OrderVO getOrderDetail(Long id);

    OrderVO payOrder(Long id);

    OrderVO cancelOrder(Long id);

    OrderVO failPayOrder(Long id);

    void cancelTimeoutOrder(Long id);
}
