package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;

public interface OrderService extends IService<Order> {

    /** 2.15 创建订单（购物车勾选下单 / 直接下单），返回订单 */
    Order createOrder(Integer buyerAccountId, String buyerUsername, CreateOrderDTO dto);

    /** 2.16 买家订单历史（时间倒序，每页固定 20 条） */
    PageResult<OrderHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size);

    /** 2.17 买家取消订单 */
    void cancelOrder(Integer buyerAccountId, Integer id);

    // ===================== 卖家端 3.12 – 3.17、3.23 =====================

    /** 3.12 商品订单记录（按创建时间升序，卡单置顶） */
    PageResult<OrderHistoryVO> getProductOrders(Integer productId, Integer page, Integer size);

    /** 3.23 顾客购买历史（订单维度） */
    PageResult<OrderHistoryVO> getCustomerOrders(Integer buyerAccountId, Integer page, Integer size);

    /** 3.13 商家确认订单（不校验库存） */
    void confirmOrder(Integer id);

    /** 3.14 标记备货完成（不校验库存） */
    void markStockDone(Integer id);

    /** 3.15 开始发货（第二道关卡：已发货量 + 本单 ≤ 库存） */
    void shipOrder(Integer id);

    /** 3.16 标记交易完成（第三道关卡：原子扣减库存） */
    void completeOrder(Integer id);

    /** 3.17 卖家取消订单（交易完成前均可） */
    void cancelOrderBySeller(Integer id);
}
