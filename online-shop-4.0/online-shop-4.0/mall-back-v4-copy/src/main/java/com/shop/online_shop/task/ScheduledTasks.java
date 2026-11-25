package com.shop.online_shop.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.PaymentRecord;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.PaymentStatus;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.PaymentRecordMapper;
import com.shop.online_shop.service.PaymentService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v4.0 后台任务（NFR-C06，调度周期 ≤ 5 分钟）：
 * ① 待支付 24h 超时自动取消（全系统唯一自动流转，零库存、零资金动作）；
 * ② 支付对账（以渠道记录为准修正支付 / 订单状态）。
 */
@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Value("${app.pending-pay-timeout-seconds:86400}")
    private long pendingPayTimeoutSeconds;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private PaymentService paymentService;

    /** 待支付超时自动取消 */
    @Scheduled(initialDelayString = "${app.task-interval-ms:300000}",
            fixedDelayString = "${app.task-interval-ms:300000}")
    @Transactional
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(pendingPayTimeoutSeconds);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
                .lt(Order::getCreatedAt, deadline);
        List<Order> candidates = orderMapper.selectList(wrapper);

        for (Order candidate : candidates) {
            Order order = orderMapper.selectByIdForUpdate(candidate.getId());
            if (order == null
                    || order.getStatus() != OrderStatus.PENDING_PAYMENT
                    || order.getCreatedAt() == null
                    || !order.getCreatedAt().isBefore(deadline)) {
                continue;
            }
            // 未支付未预占：零库存动作、零资金动作
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);

            // 进行中的支付单置为 FAIL，防止超时后回调再推进订单
            LambdaUpdateWrapper<PaymentRecord> paymentUpdate = new LambdaUpdateWrapper<>();
            paymentUpdate.eq(PaymentRecord::getOrderId, order.getId())
                    .eq(PaymentRecord::getStatus, PaymentStatus.PROCESSING)
                    .set(PaymentRecord::getStatus, PaymentStatus.FAIL);
            paymentRecordMapper.update(null, paymentUpdate);
            log.info("待支付超时自动取消：orderNo={}", order.getOrderNo());
        }
    }

    /** 支付对账：回调丢失 / 渠道记录与订单不一致时，以渠道记录为准修正 */
    @Scheduled(initialDelayString = "${app.task-interval-ms:300000}",
            fixedDelayString = "${app.task-interval-ms:300000}")
    public void reconcilePayments() {
        try {
            paymentService.reconcile();
        } catch (Exception e) {
            log.error("支付对账任务异常", e);
        }
    }
}
