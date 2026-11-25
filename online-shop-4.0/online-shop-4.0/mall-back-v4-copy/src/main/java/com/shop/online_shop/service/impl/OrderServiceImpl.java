package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.entity.Address;
import com.shop.online_shop.entity.CartItem;
import com.shop.online_shop.entity.LogisticsTracking;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.CartItemMapper;
import com.shop.online_shop.mapper.LogisticsTrackingMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.AddressService;
import com.shop.online_shop.service.AfterSalesService;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.service.ReviewService;
import com.shop.online_shop.vo.AddressSnapshotVO;
import com.shop.online_shop.vo.ConfirmReceiptVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.OrderItemVO;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ShippingVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final int HISTORY_PAGE_SIZE = 20;

    @Resource
    private ProductService productService;

    @Resource
    private CartItemMapper cartItemMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private AddressService addressService;

    @Resource
    private ReservedQuantityService reservedQuantityService;

    @Resource
    private PaymentService paymentService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private AfterSalesService afterSalesService;

    @Resource
    private LogisticsTrackingMapper logisticsTrackingMapper;

    @Override
    @Transactional
    public Order createOrder(Integer buyerAccountId, String buyerUsername, CreateOrderDTO dto) {
        List<OrderLine> lines = resolveOrderLines(buyerAccountId, dto);
        if (lines.isEmpty()) {
            throw new BusinessException("购物车为空或未勾选商品");
        }

        Address address = resolveAddress(buyerAccountId, dto.getAddressId());

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemEntities = new ArrayList<>();

        // 逐商品只读校验可卖量（账面 − 预占；不锁库存、不预占）
        for (OrderLine line : lines) {
            Product product = productService.getProductWithFill(line.productId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            if (ProductStatus.OFFLINE == product.getStatus()) {
                throw new BusinessException("商品已下架");
            }
            int stock = product.getStock() == null ? 0 : product.getStock();
            int reserved = reservedQuantityService.getReservedQuantity(product.getId());
            int available = stock - reserved;
            if (available < line.quantity()) {
                throw new BusinessException("可卖量不足：" + product.getName()
                        + "（需要 " + line.quantity() + " / 当前 " + stock + " / 可卖 " + available + "）");
            }
            // 单价取下单瞬间适用价（活动期内为活动价）
            BigDecimal unitPrice = product.getCurrentPrice() != null
                    ? product.getCurrentPrice()
                    : product.getPrice();
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(line.quantity())));

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(unitPrice);
            item.setQuantity(line.quantity());
            itemEntities.add(item);
        }

        // 生成订单：下单成功自动进入「待支付」（24h 倒计时开始）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order.setBuyerAccountId(buyerAccountId);
        order.setAddressName(address.getName());
        order.setAddressPhone(address.getPhone());
        order.setAddressProvince(address.getProvince());
        order.setAddressCity(address.getCity());
        order.setAddressDistrict(address.getDistrict());
        order.setAddressDetail(address.getDetail());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        save(order);

        for (OrderItem item : itemEntities) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 购物车下单成功后，已勾选条目移出购物车
        if (!CollectionUtils.isEmpty(dto.getCartItemIds())) {
            cartItemMapper.deleteBatchIds(dto.getCartItemIds());
        }

        order.setBuyerUsername(buyerUsername);
        List<OrderItemVO> itemVOs = itemEntities.stream().map(this::toItemVO).toList();
        order.setItems(itemVOs);
        order.setAddressSnapshot(buildAddressSnapshot(order));
        return order;
    }

    @Override
    public PageResult<OrderHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size) {
        int pageSize = HISTORY_PAGE_SIZE;
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerAccountId, buyerAccountId)
                .orderByDesc(Order::getCreatedAt)
                .orderByDesc(Order::getId);

        Page<Order> orderPage = page(new Page<>(page, pageSize), wrapper);

        List<OrderHistoryVO> list = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            list.add(buildHistoryRow(order));
        }
        return new PageResult<>(list,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    @Override
    @Transactional
    public void cancelOrder(Integer buyerAccountId, Integer id) {
        Order order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }

        OrderStatus status = order.getStatus();
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessException("退款处理中，请勿重复取消");
        }
        // 客户可取消：待支付 / 已支付 / 商家确认 / 备货完成；开始发货起不可取消
        boolean cancellable = status == OrderStatus.PENDING_PAYMENT
                || status == OrderStatus.PAID
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.STOCKED;
        if (!cancellable) {
            throw new BusinessException("当前状态不可取消");
        }

        // 已支付段取消：全额原路退款（退款幂等，预占在订单转 CANCELLED 后自动解除）
        if (status == OrderStatus.PAID
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.STOCKED) {
            paymentService.refundForOrderCancel(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public ConfirmReceiptVO confirmReceipt(Integer buyerAccountId, Integer id) {
        Order order = baseMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        // 幂等：重复调用返回成功且不产生二次扣减
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return new ConfirmReceiptVO(order.getOrderNo(), OrderStatus.COMPLETED.getCode(), true);
        }
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new BusinessException("当前状态不可确认收货");
        }

        List<OrderItem> items = itemsOf(order.getId());
        Set<Integer> productIds = new HashSet<>();
        // 预占转正式扣减：条件更新保证不超卖、不双扣
        for (OrderItem item : items) {
            int rows = productMapper.deductStock(item.getProductId(), item.getQuantity());
            if (rows == 0) {
                throw new BusinessException("库存校验失败，请联系卖家处理");
            }
            productIds.add(item.getProductId());
        }
        // 扣减后库存为 0 → 自动下架（成交扣减导致的自动下架不受已支付商品保护限制）
        for (Integer productId : productIds) {
            productMapper.autoOfflineIfZero(productId);
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        return new ConfirmReceiptVO(order.getOrderNo(), OrderStatus.COMPLETED.getCode(), true);
    }

    @Override
    public ShippingVO getShipping(Integer buyerAccountId, Integer id) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        if (!StringUtils.hasText(order.getWaybillNo())) {
            throw new BusinessException("订单尚未发货");
        }

        LambdaQueryWrapper<LogisticsTracking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogisticsTracking::getOrderId, order.getId())
                .orderByDesc(LogisticsTracking::getTrackedAt)
                .orderByDesc(LogisticsTracking::getId);
        List<LogisticsTracking> trackings = logisticsTrackingMapper.selectList(wrapper);

        ShippingVO vo = new ShippingVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setLogisticsProvider(order.getLogisticsProvider());
        vo.setWaybillNo(order.getWaybillNo());
        List<ShippingVO.TrackingItem> items = new ArrayList<>();
        for (LogisticsTracking tracking : trackings) {
            items.add(new ShippingVO.TrackingItem(
                    tracking.getStatus() == null ? null : tracking.getStatus().getCode(),
                    tracking.getDescription(),
                    tracking.getTrackedAt()));
        }
        vo.setTracking(items);
        return vo;
    }

    // ===================== 内部方法 =====================

    private OrderHistoryVO buildHistoryRow(Order order) {
        List<OrderItem> items = itemsOf(order.getId());
        List<Integer> itemIds = items.stream().map(OrderItem::getId).toList();
        Set<Integer> reviewed = reviewService.getReviewedItemIds(itemIds);
        Set<Integer> refundClosed = afterSalesService.getRefundClosedItemIds(itemIds);
        Set<Integer> ongoing = afterSalesService.getOngoingItemIds(itemIds);

        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO vo = toItemVO(item);
            vo.setReviewed(reviewed.contains(item.getId()));
            vo.setRefundClosed(refundClosed.contains(item.getId()));
            vo.setAfterSalesOngoing(ongoing.contains(item.getId()));
            itemVOs.add(vo);
        }

        OrderStatus status = order.getStatus();
        boolean canReview = status == OrderStatus.COMPLETED && itemVOs.stream()
                .anyMatch(item -> !Boolean.TRUE.equals(item.getReviewed())
                        && !Boolean.TRUE.equals(item.getRefundClosed()));

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
        vo.setCanReview(canReview);
        return vo;
    }

    private Address resolveAddress(Integer buyerAccountId, Integer addressId) {
        if (addressId != null) {
            Address address = addressService.getById(addressId);
            if (address == null || !buyerAccountId.equals(address.getBuyerAccountId())) {
                throw new BusinessException("收货地址不存在");
            }
            return address;
        }
        Address defaultAddress = addressService.getDefault(buyerAccountId);
        if (defaultAddress == null) {
            throw new BusinessException("收货地址不存在");
        }
        return defaultAddress;
    }

    /**
     * 解析下单明细：购物车勾选下单 / 商品详情页直接下单
     */
    private List<OrderLine> resolveOrderLines(Integer buyerAccountId, CreateOrderDTO dto) {
        List<OrderLine> lines = new ArrayList<>();

        if (!CollectionUtils.isEmpty(dto.getCartItemIds())) {
            List<CartItem> cartItems = cartItemMapper.selectBatchIds(dto.getCartItemIds());
            for (CartItem cartItem : cartItems) {
                if (!buyerAccountId.equals(cartItem.getBuyerAccountId())) {
                    throw new BusinessException("无权访问");
                }
                lines.add(new OrderLine(cartItem.getProductId(), cartItem.getQuantity()));
            }
        } else if (dto.getProductId() != null) {
            if (dto.getQuantity() == null || dto.getQuantity() < 1) {
                throw new BusinessException("数量必须大于0");
            }
            lines.add(new OrderLine(dto.getProductId(), dto.getQuantity()));
        }
        return lines;
    }

    private List<OrderItem> itemsOf(Integer orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId);
        return orderItemMapper.selectList(wrapper);
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

    /**
     * 订单号：日期(yyyyMMdd) + "-" + 4 位流水
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Order::getOrderNo, datePart + "-");
        long count = count(wrapper);
        return datePart + "-" + String.format("%04d", count + 1);
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setReviewed(false);
        vo.setRefundClosed(false);
        vo.setAfterSalesOngoing(false);
        return vo;
    }

    /** 下单明细行 */
    private record OrderLine(Integer productId, Integer quantity) {
    }
}
