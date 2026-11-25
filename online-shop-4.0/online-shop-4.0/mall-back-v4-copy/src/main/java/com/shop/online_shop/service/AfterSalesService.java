package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.AfterSalesApplyDTO;
import com.shop.online_shop.entity.AfterSalesOrder;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.PageResult;

import java.util.Collection;
import java.util.Set;

public interface AfterSalesService extends IService<AfterSalesOrder> {

    /** 2.27 按订单明细发起售后申请 */
    AfterSalesOrder apply(Integer buyerAccountId, Integer itemId, AfterSalesApplyDTO dto);

    /** 2.28 我的售后单列表 */
    PageResult<AfterSalesOrderVO> listMine(Integer buyerAccountId, Integer page, Integer size);

    /** 该明细是否已有进行中的售后单（申请中 / 协商中） */
    Set<Integer> getOngoingItemIds(Collection<Integer> orderItemIds);

    /** 已退款闭环（退货退款 / 仅退款）的明细 ID 集合 */
    Set<Integer> getRefundClosedItemIds(Collection<Integer> orderItemIds);
}
