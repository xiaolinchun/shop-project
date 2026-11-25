package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.ShipDTO;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.LogisticsProvider;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.mapper.BuyerAccountMapper;
import com.shop.online_shop.mapper.LogisticsProviderMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.service.SellerOrderService;
import com.shop.online_shop.vo.AddressSnapshotVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.OrderItemVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SellerOrderServiceImpl implements SellerOrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private BuyerAccountMapper buyerAccountMapper;

    @Resource
    private LogisticsProviderMapper logisticsProviderMapper;

    @Resource
    private PaymentService paymentService;

    @Resource
    private ReservedQuantityService reservedQuantityService;

    @Override
    public PageResult<OrderHistoryVO> productOrders(String sellerId, Integer productId, Integer page, Integer size) {
        if (productId == null) {
            throw new BusinessException("商品不存在");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!sellerId.equals(product.getSellerId())) {
            throw new BusinessException("无权访问");
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(Order::getId,
                        "SELECT order_id FROM order_item WHERE product_id = " + productId)
                .orderByAsc(Order::getCreatedAt)
                .orderByAsc(Order::getId);
        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        int reserved = reservedQuantityService.getReservedQuantity(productId);
        int stock = product.getStock() == null ? 0 : product.getStock();

        List<OrderHistoryVO> list = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            OrderHistoryVO vo = buildHistoryRow(order);
            vo.setReservedQuantity(reserved);
            vo.setAvailableQuantity(stock - reserved);
            list.add(vo);
        }
        return new PageResult<>(list,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    @Override
    @Transactional
    public void confirm(String sellerId, Integer id) {
        Order order = requireOwned(sellerId, id, true);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("当前状态不允许该操作");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void stockDone(String sellerId, Integer id) {
        Order order = requireOwned(sellerId, id, true);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("当前状态不允许该操作");
        }
        order.setStatus(OrderStatus.STOCKED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void ship(String sellerId, Integer id, ShipDTO dto) {
        Order order = requireOwned(sellerId, id, true);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单预占已释放，无法发货");
        }
        if (order.getStatus() != OrderStatus.STOCKED) {
            throw new BusinessException("当前状态不允许该操作");
        }
        if (!StringUtils.hasText(dto.getWaybillNo())) {
            throw new BusinessException("运单号不能为空");
        }
        LogisticsProvider provider = dto.getLogisticsProviderId() == null
                ? null
                : logisticsProviderMapper.selectById(dto.getLogisticsProviderId());
        if (provider == null) {
            throw new BusinessException("物流服务商不存在");
        }

        order.setLogisticsProvider(provider.getName());
        order.setWaybillNo(dto.getWaybillNo().trim());
        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(String sellerId, Integer id) {
        Order order = requireOwned(sellerId, id, true);
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.CANCELLED || status == OrderStatus.COMPLETED) {
            throw new BusinessException("当前状态不可取消");
        }

        // 已支付段取消：全额原路退款（预占在订单转 CANCELLED 后自动解除）
        if (status == OrderStatus.PAID
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.STOCKED
                || status == OrderStatus.SHIPPING) {
            paymentService.refundForOrderCancel(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public PageResult<CustomerVO> customers(Integer page, Integer size) {
        Page<BuyerAccount> buyerPage = buyerAccountMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BuyerAccount>()
                        .orderByDesc(BuyerAccount::getCreatedAt)
                        .orderByDesc(BuyerAccount::getId));

        List<CustomerVO> list = new ArrayList<>();
        for (BuyerAccount buyer : buyerPage.getRecords()) {
            CustomerVO vo = new CustomerVO();
            vo.setUsername(buyer.getUsername());
            vo.setPhone(buyer.getPhone());
            vo.setDefaultTradeLocation(buyer.getDefaultTradeLocation());
            vo.setCreatedAt(buyer.getCreatedAt());
            list.add(vo);
        }
        return new PageResult<>(list,
                (int) buyerPage.getTotal(),
                (int) buyerPage.getCurrent(),
                (int) buyerPage.getSize());
    }

    @Override
    public PageResult<OrderHistoryVO> customerHistory(String username, Integer page, Integer size) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("顾客不存在");
        }
        BuyerAccount buyer = buyerAccountMapper.selectOne(new LambdaQueryWrapper<BuyerAccount>()
                .eq(BuyerAccount::getUsername, username.trim())
                .last("LIMIT 1"));
        if (buyer == null) {
            throw new BusinessException("顾客不存在");
        }

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerAccountId, buyer.getId())
                        .orderByDesc(Order::getCreatedAt)
                        .orderByDesc(Order::getId));

        List<OrderHistoryVO> list = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            list.add(buildHistoryRow(order));
        }
        return new PageResult<>(list,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    private Order requireOwned(String sellerId, Integer id, boolean forUpdate) {
        Order order = id == null
                ? null
                : (forUpdate ? orderMapper.selectByIdForUpdate(id) : orderMapper.selectById(id));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 订单归属校验：全部 order_item 对应的 product 都必须属于当前卖家
        // （跨卖家混单不在卖家端处理范围内，由平台侧协调）
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        if (items.isEmpty()) {
            throw new BusinessException("订单明细不存在");
        }
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                throw new BusinessException("订单明细商品不存在");
            }
            if (!sellerId.equals(product.getSellerId())) {
                throw new BusinessException("无权访问");
            }
        }
        return order;
    }

    private OrderHistoryVO buildHistoryRow(Order order) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId()).orderByAsc(OrderItem::getId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO vo = new OrderItemVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductName(item.getProductName());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setQuantity(item.getQuantity());
            vo.setReviewed(false);
            vo.setRefundClosed(false);
            vo.setAfterSalesOngoing(false);
            itemVOs.add(vo);
        }

        OrderStatus status = order.getStatus();
        OrderHistoryVO vo = new OrderHistoryVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setItems(itemVOs);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(status == null ? null : status.getCode());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPaymentChannel(order.getPaymentChannel());
        vo.setPaidAt(order.getPaidAt());
        vo.setAddressSnapshot(buildAddressSnapshot(order));
        vo.setLogisticsProvider(order.getLogisticsProvider());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setCanPay(status == OrderStatus.PENDING_PAYMENT);
        vo.setCanConfirmReceipt(status == OrderStatus.SHIPPING);
        vo.setShippingQuery(StringUtils.hasText(order.getWaybillNo()));
        vo.setCanAfterSales(status == OrderStatus.SHIPPING || status == OrderStatus.COMPLETED);
        vo.setCanReview(false);
        vo.setStayDays(stayDays(order));
        return vo;
    }

    private AddressSnapshotVO buildAddressSnapshot(Order order) {
        return new AddressSnapshotVO(
                order.getAddressName(),
                order.getAddressPhone(),
                order.getAddressProvince(),
                order.getAddressCity(),
                order.getAddressDistrict(),
                order.getAddressDetail());
    }

    private Integer stayDays(Order order) {
        LocalDateTime from = order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt();
        if (from == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(from, LocalDateTime.now());
        return (int) Math.max(0, days);
    }
}
