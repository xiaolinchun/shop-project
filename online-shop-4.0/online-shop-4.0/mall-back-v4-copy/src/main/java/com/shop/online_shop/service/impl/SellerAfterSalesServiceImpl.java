package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.AfterSalesResultDTO;
import com.shop.online_shop.dto.seller.ExchangeShipDTO;
import com.shop.online_shop.dto.seller.ReturnReceivedDTO;
import com.shop.online_shop.entity.AfterSalesOrder;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.RefundRecord;
import com.shop.online_shop.enums.AfterSalesResult;
import com.shop.online_shop.enums.AfterSalesStatus;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.AfterSalesOrderMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.mapper.RefundRecordMapper;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.service.SellerAfterSalesService;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SellerAfterSalesServiceImpl implements SellerAfterSalesService {

    @Resource
    private AfterSalesOrderMapper afterSalesOrderMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RefundRecordMapper refundRecordMapper;

    @Resource
    private PaymentService paymentService;

    @Resource
    private ReservedQuantityService reservedQuantityService;

    @Override
    public PageResult<AfterSalesOrderVO> list(Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        Page<AfterSalesOrder> afterSalesPage = afterSalesOrderMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<AfterSalesOrder>()
                        .orderByDesc(AfterSalesOrder::getCreatedAt)
                        .orderByDesc(AfterSalesOrder::getId));

        List<AfterSalesOrderVO> list = new ArrayList<>();
        for (AfterSalesOrder afterSales : afterSalesPage.getRecords()) {
            Order order = orderMapper.selectById(afterSales.getOrderId());
            AfterSalesOrderVO vo = new AfterSalesOrderVO();
            vo.setId(afterSales.getId());
            vo.setAfterSalesNo(afterSales.getAfterSalesNo());
            vo.setOrderNo(order == null ? null : order.getOrderNo());
            vo.setItemId(afterSales.getOrderItemId());
            vo.setProductId(afterSales.getProductId());
            vo.setProductName(afterSales.getProductName());
            vo.setTitle(afterSales.getTitle());
            vo.setDescription(afterSales.getDescription());
            vo.setImages(afterSales.getImages());
            vo.setStatus(afterSales.getStatus() == null ? null : afterSales.getStatus().getCode());
            vo.setResult(afterSales.getResult() == null ? null : afterSales.getResult().getCode());
            vo.setResultAt(afterSales.getResultAt());
            vo.setCreatedAt(afterSales.getCreatedAt());
            vo.setReturnWaybillNo(afterSales.getReturnWaybillNo());
            vo.setExchangeWaybillNo(afterSales.getExchangeWaybillNo());
            list.add(vo);
        }
        return new PageResult<>(list,
                (int) afterSalesPage.getTotal(),
                (int) afterSalesPage.getCurrent(),
                (int) afterSalesPage.getSize());
    }

    @Override
    @Transactional
    public void negotiating(Integer id) {
        AfterSalesOrder afterSales = requireAfterSales(id);
        if (afterSales.getStatus() != AfterSalesStatus.APPLYING) {
            throw new BusinessException("当前状态不允许该操作");
        }
        afterSales.setStatus(AfterSalesStatus.NEGOTIATING);
        afterSales.setUpdatedAt(LocalDateTime.now());
        afterSalesOrderMapper.updateById(afterSales);
    }

    @Override
    @Transactional
    public Map<String, Object> recordResult(Integer id, AfterSalesResultDTO dto) {
        AfterSalesOrder afterSales = requireAfterSales(id);
        if (afterSales.getStatus() == AfterSalesStatus.CLOSED) {
            throw new BusinessException("当前状态不允许录入结果");
        }
        AfterSalesResult result = parseResult(dto.getResult());

        Order order = requireOrderForUpdate(afterSales.getOrderId());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单已取消，不再受理售后");
        }
        AfterSalesResult effective = result;

        // 换货兜底：商品已下架或可卖量为 0 → 自动转为仅退款
        if (result == AfterSalesResult.EXCHANGE) {
            Product product = productMapper.selectByIdForUpdate(afterSales.getProductId());
            boolean unavailable;
            if (product == null) {
                unavailable = true;
            } else {
                int stock = product.getStock() == null ? 0 : product.getStock();
                int reserved = reservedQuantityService.getReservedQuantity(product.getId());
                unavailable = product.getStatus() == ProductStatus.OFFLINE || stock - reserved <= 0;
            }
            if (unavailable) {
                effective = AfterSalesResult.REFUND_ONLY;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        afterSales.setResult(effective);
        afterSales.setStatus(AfterSalesStatus.CLOSED);
        afterSales.setResultAt(now);
        afterSales.setUpdatedAt(now);
        afterSalesOrderMapper.updateById(afterSales);

        // 仅退款：立即原路退款；若订单处于开始发货，同步解除预占并转已取消
        if (effective == AfterSalesResult.REFUND_ONLY) {
            refundOnly(order, afterSales);
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("status", AfterSalesStatus.CLOSED.getCode());
        resultMap.put("result", effective.getCode());
        return resultMap;
    }

    @Override
    @Transactional
    public Map<String, Object> returnReceived(Integer id, ReturnReceivedDTO dto) {
        AfterSalesOrder afterSales = requireAfterSales(id);
        if (afterSales.getResult() != AfterSalesResult.REFUND_AND_RETURN) {
            throw new BusinessException("售后结果非退货退款");
        }
        // 幂等拦截：同一售后单已退款则视为已确认收货
        if (hasRefund(afterSales.getAfterSalesNo())) {
            throw new BusinessException("已确认收货，请勿重复操作");
        }

        Order order = requireOrderForUpdate(afterSales.getOrderId());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单已取消，不再受理退货退款");
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.COMPLETED) {
            // 交易完成态退货：库存回补，订单保持交易完成
            OrderItem item = afterSales.getOrderItemId() == null
                    ? null
                    : orderItemMapper.selectById(afterSales.getOrderItemId());
            if (item == null) {
                throw new BusinessException("订单明细不存在");
            }
            productMapper.addStock(item.getProductId(), item.getQuantity());
        } else if (status == OrderStatus.SHIPPING) {
            // 开始发货态退货：解除预占，订单转已取消
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
        } else {
            throw new BusinessException("当前状态不允许该操作");
        }

        if (StringUtils.hasText(dto.getReturnWaybillNo())) {
            afterSales.setReturnWaybillNo(dto.getReturnWaybillNo().trim());
        }
        afterSales.setUpdatedAt(LocalDateTime.now());
        afterSalesOrderMapper.updateById(afterSales);

        RefundRecord refund = paymentService.refundForAfterSales(order, afterSales.getAfterSalesNo());

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("refundNo", refund == null ? null : refund.getRefundNo());
        resultMap.put("orderStatus", order.getStatus() == null ? null : order.getStatus().getCode());
        return resultMap;
    }

    @Override
    @Transactional
    public void exchangeShip(Integer id, ExchangeShipDTO dto) {
        AfterSalesOrder afterSales = requireAfterSales(id);
        if (afterSales.getResult() != AfterSalesResult.EXCHANGE) {
            throw new BusinessException("售后结果非换货");
        }
        if (!StringUtils.hasText(dto.getReturnWaybillNo())
                || !StringUtils.hasText(dto.getExchangeWaybillNo())) {
            throw new BusinessException("运单号不能为空");
        }
        afterSales.setReturnWaybillNo(dto.getReturnWaybillNo().trim());
        afterSales.setExchangeWaybillNo(dto.getExchangeWaybillNo().trim());
        afterSales.setUpdatedAt(LocalDateTime.now());
        afterSalesOrderMapper.updateById(afterSales);
    }

    private void refundOnly(Order order, AfterSalesOrder afterSales) {
        paymentService.refundForAfterSales(order, afterSales.getAfterSalesNo());
        // CA-C7 / A-CL-C22：仅退款等同取消——所有未终态订单在退款后统一进入 CANCELLED
        // 终态 COMPLETED 上的仅退款（极端入口）不改订单状态，退款只记在售后单
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.PAID
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.STOCKED
                || status == OrderStatus.SHIPPING) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    private boolean hasRefund(String afterSalesNo) {
        return refundRecordMapper.selectCount(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSalesNo, afterSalesNo)) > 0;
    }

    private AfterSalesOrder requireAfterSales(Integer id) {
        AfterSalesOrder afterSales = id == null ? null : afterSalesOrderMapper.selectById(id);
        if (afterSales == null) {
            throw new BusinessException("售后单不存在");
        }
        return afterSales;
    }

    private Order requireOrderForUpdate(Integer orderId) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private AfterSalesResult parseResult(String code) {
        if (StringUtils.hasText(code)) {
            for (AfterSalesResult result : AfterSalesResult.values()) {
                if (result.getCode().equalsIgnoreCase(code.trim())) {
                    return result;
                }
            }
        }
        throw new BusinessException("不支持的售后结果");
    }
}
