package com.shop.online_shop.service;

import com.shop.online_shop.dto.seller.ShipDTO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;

public interface SellerOrderService {

    /** 3.12 商品订单列表（按商品维度） */
    PageResult<OrderHistoryVO> productOrders(String sellerId, Integer productId, Integer page, Integer size);

    /** 3.13 确认订单（校验订单商品归属 sellerId） */
    void confirm(String sellerId, Integer id);

    /** 3.14 备货完成（校验归属） */
    void stockDone(String sellerId, Integer id);

    /** 3.15 开始发货（校验归属） */
    void ship(String sellerId, Integer id, ShipDTO dto);

    /** 3.17 取消订单（校验归属） */
    void cancel(String sellerId, Integer id);

    /** 3.22 客户列表 */
    PageResult<CustomerVO> customers(Integer page, Integer size);

    /** 3.23 查看客户历史订单 */
    PageResult<OrderHistoryVO> customerHistory(String username, Integer page, Integer size);
}
