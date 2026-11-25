package com.shop.online_shop.service;

import com.shop.online_shop.dto.seller.AfterSalesResultDTO;
import com.shop.online_shop.dto.seller.ExchangeShipDTO;
import com.shop.online_shop.dto.seller.ReturnReceivedDTO;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.PageResult;

import java.util.Map;

public interface SellerAfterSalesService {

    /** 3.28 售后单列表（按申请时间倒序） */
    PageResult<AfterSalesOrderVO> list(Integer page, Integer size);

    /** 3.29 协商中 */
    void negotiating(Integer id);

    /** 3.30 录入售后结果 */
    Map<String, Object> recordResult(Integer id, AfterSalesResultDTO dto);

    /** 3.31 确认收到退回件 */
    Map<String, Object> returnReceived(Integer id, ReturnReceivedDTO dto);

    /** 3.32 换货寄出登记 */
    void exchangeShip(Integer id, ExchangeShipDTO dto);
}
