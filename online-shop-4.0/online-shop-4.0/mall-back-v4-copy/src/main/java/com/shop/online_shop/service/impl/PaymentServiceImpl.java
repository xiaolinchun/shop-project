package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.payment.PaymentNotifyDTO;
import com.shop.online_shop.entity.MockChannelTxn;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.PaymentRecord;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.RefundRecord;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.PaymentChannel;
import com.shop.online_shop.enums.PaymentStatus;
import com.shop.online_shop.enums.RefundStatus;
import com.shop.online_shop.mapper.MockChannelTxnMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.PaymentRecordMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.mapper.RefundRecordMapper;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.vo.PaymentNotifyResultVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAIL = "FAIL";

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private RefundRecordMapper refundRecordMapper;

    @Resource
    private MockChannelTxnMapper mockChannelTxnMapper;

    @Override
    @Transactional
    public PaymentRecord initiate(Integer buyerAccountId, Integer orderId, String channelCode) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("订单当前状态不可支付");
        }
        PaymentChannel channel = PaymentChannel.fromCode(channelCode);
        if (channel == null) {
            throw new BusinessException("不支持的支付渠道");
        }
        if (paymentRecordMapper.selectActiveByOrderId(orderId) != null) {
            throw new BusinessException("该订单已有进行中的支付");
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(order.getOrderNo());
        payment.setOrderId(order.getId());
        payment.setChannel(channel);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(payment);
        return payment;
    }

    @Override
    @Transactional
    public PaymentNotifyResultVO notify(PaymentNotifyDTO dto) {
        if (!StringUtils.hasText(dto.getPaymentNo())) {
            throw new BusinessException("支付单不存在");
        }
        PaymentRecord payment = paymentRecordMapper.selectByPaymentNoForUpdate(dto.getPaymentNo());
        if (payment == null) {
            throw new BusinessException("支付单不存在");
        }
        PaymentChannel channel = PaymentChannel.fromCode(dto.getChannel());
        if (channel == null) {
            throw new BusinessException("不支持的支付渠道");
        }
        if (payment.getChannel() != null && payment.getChannel() != channel) {
            throw new BusinessException("不支持的支付渠道");
        }

        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 幂等：同一 paymentNo 已处理完成，不再二次预占 / 二次退款
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.FAIL) {
            return new PaymentNotifyResultVO(order.getOrderNo(), order.getStatus().getCode(),
                    payment.getStatus() == PaymentStatus.REFUNDED, "支付单已完成，忽略重复回调");
        }

        recordMockChannelTxn(payment, dto);

        if (!RESULT_SUCCESS.equals(dto.getResult())) {
            payment.setStatus(PaymentStatus.FAIL);
            payment.setChannelTxnNo(dto.getChannelTxnNo());
            paymentRecordMapper.updateById(payment);
            return new PaymentNotifyResultVO(order.getOrderNo(), order.getStatus().getCode(), false, null);
        }

        LocalDateTime paidAt = dto.getPaidAt() == null ? LocalDateTime.now() : dto.getPaidAt();

        // 锁订单（与取消 / 确认收货互斥）
        Order lockedOrder = orderMapper.selectByIdForUpdate(order.getId());
        if (lockedOrder.getStatus() != OrderStatus.PENDING_PAYMENT) {
            // 渠道已扣款但订单已不可支付：立即全额原路退款
            markPaymentSuccess(payment, dto, paidAt);
            createRefund(payment, lockedOrder, null);
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRecordMapper.updateById(payment);
            return new PaymentNotifyResultVO(lockedOrder.getOrderNo(), lockedOrder.getStatus().getCode(),
                    true, "订单当前状态不可支付，已全额原路退款");
        }

        // 原子「可卖量校验 + 预占」：先按商品 ID 升序锁商品行，再计算可卖量
        List<OrderItem> items = itemsOf(lockedOrder.getId());
        items.sort(Comparator.comparing(OrderItem::getProductId));
        for (OrderItem item : items) {
            Product product = productMapper.selectByIdForUpdate(item.getProductId());
            if (product == null) {
                return refundAndStayPending(payment, lockedOrder, dto, paidAt);
            }
            int reserved = orderItemMapper.sumReservedByProductId(item.getProductId());
            int available = product.getStock() - reserved;
            if (available < item.getQuantity()) {
                return refundAndStayPending(payment, lockedOrder, dto, paidAt);
            }
        }

        // 预占成功：登记占用标记（预占为派生值，仅将订单置为 PAID 即生效）
        markPaymentSuccess(payment, dto, paidAt);
        lockedOrder.setStatus(OrderStatus.PAID);
        lockedOrder.setPaymentChannel(channel);
        lockedOrder.setPaidAt(paidAt);
        lockedOrder.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(lockedOrder);
        return new PaymentNotifyResultVO(lockedOrder.getOrderNo(), OrderStatus.PAID.getCode(), false, null);
    }

    @Override
    @Transactional
    public RefundRecord refundForOrderCancel(Order order) {
        PaymentRecord payment = paymentRecordMapper.selectSuccessByOrderId(order.getId());
        if (payment == null) {
            return null;
        }
        RefundRecord refund = createRefund(payment, order, null);
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRecordMapper.updateById(payment);
        return refund;
    }

    @Override
    @Transactional
    public RefundRecord refundForAfterSales(Order order, String afterSalesNo) {
        PaymentRecord payment = paymentRecordMapper.selectSuccessByOrderId(order.getId());
        if (payment == null) {
            // 同一订单的多笔售后：支付单已退款时沿用该支付单生成售后维度的退款记录
            payment = paymentRecordMapper.selectRefundedByOrderId(order.getId());
        }
        if (payment == null) {
            return null;
        }
        RefundRecord refund = createRefund(payment, order, afterSalesNo);
        if (payment.getStatus() != PaymentStatus.REFUNDED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRecordMapper.updateById(payment);
        }
        return refund;
    }

    @Override
    @Transactional
    public void reconcile() {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getStatus, PaymentStatus.PROCESSING);
        List<PaymentRecord> processing = paymentRecordMapper.selectList(wrapper);
        for (PaymentRecord payment : processing) {
            MockChannelTxn txn = mockChannelTxnMapper.selectLatestByPaymentNo(payment.getPaymentNo());
            if (txn == null) {
                continue;
            }
            if (RESULT_SUCCESS.equals(txn.getMockResult())) {
                PaymentNotifyDTO dto = new PaymentNotifyDTO();
                dto.setPaymentNo(txn.getPaymentNo());
                dto.setChannel(txn.getChannel());
                dto.setChannelTxnNo(txn.getChannelTxnNo());
                dto.setResult(RESULT_SUCCESS);
                dto.setPaidAt(txn.getCallbackAt());
                dto.setAmount(txn.getAmount());
                notify(dto);
            } else if (RESULT_FAIL.equals(txn.getMockResult())) {
                payment.setStatus(PaymentStatus.FAIL);
                payment.setChannelTxnNo(txn.getChannelTxnNo());
                paymentRecordMapper.updateById(payment);
            }
        }
    }

    private PaymentNotifyResultVO refundAndStayPending(PaymentRecord payment, Order order,
                                                       PaymentNotifyDTO dto, LocalDateTime paidAt) {
        // 渠道已扣款但可卖量不足：支付不成功，立即全额原路退款，订单停留待支付（收不到钱即无资损）
        markPaymentSuccess(payment, dto, paidAt);
        createRefund(payment, order, null);
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRecordMapper.updateById(payment);
        return new PaymentNotifyResultVO(order.getOrderNo(), OrderStatus.PENDING_PAYMENT.getCode(),
                true, "可卖量不足，已全额原路退款，订单停留待支付");
    }

    private void markPaymentSuccess(PaymentRecord payment, PaymentNotifyDTO dto, LocalDateTime paidAt) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setChannelTxnNo(dto.getChannelTxnNo());
        payment.setPaidAt(paidAt);
        paymentRecordMapper.updateById(payment);
    }

    private RefundRecord createRefund(PaymentRecord payment, Order order, String afterSalesNo) {
        // 幂等：同一支付单 + 同一售后单（取消退款为 null）只生成一条退款记录
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefundRecord::getPaymentNo, payment.getPaymentNo())
                .isNull(afterSalesNo == null, RefundRecord::getAfterSalesNo)
                .eq(afterSalesNo != null, RefundRecord::getAfterSalesNo, afterSalesNo)
                .last("LIMIT 1");
        RefundRecord existing = refundRecordMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setAfterSalesNo(afterSalesNo);
        refund.setOrderId(order.getId());
        refund.setAmount(payment.getAmount());
        refund.setChannel(payment.getChannel());
        refund.setStatus(RefundStatus.SUCCESS);
        refund.setCreatedAt(LocalDateTime.now());
        refundRecordMapper.insert(refund);
        return refund;
    }

    private void recordMockChannelTxn(PaymentRecord payment, PaymentNotifyDTO dto) {
        if (!StringUtils.hasText(dto.getChannelTxnNo())) {
            return;
        }
        LambdaQueryWrapper<MockChannelTxn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MockChannelTxn::getChannelTxnNo, dto.getChannelTxnNo());
        if (mockChannelTxnMapper.selectCount(wrapper) > 0) {
            return;
        }
        MockChannelTxn txn = new MockChannelTxn();
        txn.setChannelTxnNo(dto.getChannelTxnNo());
        txn.setPaymentNo(payment.getPaymentNo());
        txn.setChannel(dto.getChannel());
        txn.setAmount(dto.getAmount() == null ? payment.getAmount() : dto.getAmount());
        txn.setMockResult(dto.getResult());
        txn.setCallbackAt(LocalDateTime.now());
        txn.setCreatedAt(LocalDateTime.now());
        mockChannelTxnMapper.insert(txn);
    }

    private List<OrderItem> itemsOf(Integer orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId);
        return orderItemMapper.selectList(wrapper);
    }

    private String generatePaymentNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PaymentRecord::getPaymentNo, "PAY" + datePart);
        long count = paymentRecordMapper.selectCount(wrapper);
        return "PAY" + datePart + String.format("%04d", count + 1);
    }

    private String generateRefundNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(RefundRecord::getRefundNo, "RF" + datePart);
        long count = refundRecordMapper.selectCount(wrapper);
        return "RF" + datePart + String.format("%04d", count + 1);
    }
}
