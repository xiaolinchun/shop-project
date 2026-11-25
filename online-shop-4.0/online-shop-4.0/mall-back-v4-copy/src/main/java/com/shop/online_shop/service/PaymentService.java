package com.shop.online_shop.service;

import com.shop.online_shop.dto.payment.PaymentNotifyDTO;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.PaymentRecord;
import com.shop.online_shop.entity.RefundRecord;
import com.shop.online_shop.vo.PaymentNotifyResultVO;

public interface PaymentService {

    /** 2.19 发起支付（生成 PROCESSING 支付单，同订单同时仅一笔有效支付） */
    PaymentRecord initiate(Integer buyerAccountId, Integer orderId, String channel);

    /** 4.1 支付渠道回调（幂等；原子执行可卖量校验 + 预占） */
    PaymentNotifyResultVO notify(PaymentNotifyDTO dto);

    /** 取消订单时全额原路退款（幂等）；无成功支付时返回 null */
    RefundRecord refundForOrderCancel(Order order);

    /** 售后闭环时全额原路退款（按售后单号幂等）；无成功支付时返回 null */
    RefundRecord refundForAfterSales(Order order, String afterSalesNo);

    /** 支付对账（以渠道记录为准修正订单状态） */
    void reconcile();
}
