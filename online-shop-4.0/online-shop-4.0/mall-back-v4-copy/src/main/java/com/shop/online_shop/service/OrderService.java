package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.vo.ConfirmReceiptVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ShippingVO;

public interface OrderService extends IService<Order> {

    /** 2.15 创建订单（购物车勾选下单 / 直接下单），下单成功进入待支付 */
    Order createOrder(Integer buyerAccountId, String buyerUsername, CreateOrderDTO dto);

    /** 2.16 买家订单历史（时间倒序，每页固定 20 条） */
    PageResult<OrderHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size);

    /** 2.17 买家取消订单（已支付段取消即全额原路退款 + 解除预占） */
    void cancelOrder(Integer buyerAccountId, Integer id);

    /** 2.20 确认收货（SHIPPING → COMPLETED，预占转正式扣减） */
    ConfirmReceiptVO confirmReceipt(Integer buyerAccountId, Integer id);

    /** 2.26 物流状态查询（运单号 + 跟踪信息，不自动推进订单状态） */
    ShippingVO getShipping(Integer buyerAccountId, Integer id);
}
