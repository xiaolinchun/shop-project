package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.entity.CartItem;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.CartItemMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.OrderItemVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final int HISTORY_PAGE_SIZE = 20;

    /** 卡单置顶：发货被拒的订单排在同状态订单之前，其余按创建时间升序 */
    private static final Comparator<OrderHistoryVO> BLOCKED_FIRST_TIME_ASC =
            Comparator.comparingInt((OrderHistoryVO vo) -> Boolean.TRUE.equals(vo.getShipBlocked()) ? 0 : 1)
                    .thenComparing(OrderHistoryVO::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    /** 卡单置顶 + 时间倒序（顾客购买历史口径） */
    private static final Comparator<OrderHistoryVO> BLOCKED_FIRST_TIME_DESC =
            Comparator.comparingInt((OrderHistoryVO vo) -> Boolean.TRUE.equals(vo.getShipBlocked()) ? 0 : 1)
                    .thenComparing(OrderHistoryVO::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()));

    @Resource
    private ProductService productService;

    @Resource
    private CartItemMapper cartItemMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    @Transactional
    public Order createOrder(Integer buyerAccountId, String buyerUsername, CreateOrderDTO dto) {
        // 收集下单明细：productId -> quantity
        List<OrderLine> lines = resolveOrderLines(buyerAccountId, dto);
        if (lines.isEmpty()) {
            throw new BusinessException("购物车为空或未勾选商品");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> itemEntities = new ArrayList<>();

        // 逐商品校验库存（第一道关卡，只读校验不扣减）
        for (OrderLine line : lines) {
            Product product = productService.getProductWithFill(line.productId);
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            if (ProductStatus.OFFLINE == product.getStatus()) {
                throw new BusinessException("商品已下架");
            }
            if (product.getStock() == null || product.getStock() < line.quantity) {
                throw new BusinessException("库存不足：" + product.getName()
                        + "（需要 " + line.quantity + " / 当前 "
                        + (product.getStock() == null ? 0 : product.getStock()) + "）");
            }
            // 单价取下单瞬间适用价（活动期内为活动价）
            BigDecimal unitPrice = product.getCurrentPrice() != null
                    ? product.getCurrentPrice()
                    : product.getPrice();
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(line.quantity)));

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(unitPrice);
            item.setQuantity(line.quantity);
            itemEntities.add(item);
        }

        // 生成订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(totalAmount);
        order.setBuyerAccountId(buyerAccountId);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        save(order);

        // 保存明细
        for (OrderItem item : itemEntities) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 购物车下单成功后，已勾选条目移出购物车
        if (dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            cartItemMapper.deleteBatchIds(dto.getCartItemIds());
        }

        // 组装返回
        order.setBuyerUsername(buyerUsername);
        order.setItems(itemEntities.stream().map(this::toItemVO).toList());
        return order;
    }

    @Override
    public PageResult<OrderHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size) {
        // 每页固定 20 条，传入其他值忽略
        int pageSize = HISTORY_PAGE_SIZE;

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerAccountId, buyerAccountId)
                .orderByDesc(Order::getCreatedAt)
                .orderByDesc(Order::getId);

        Page<Order> orderPage = page(new Page<>(page, pageSize), wrapper);

        List<OrderHistoryVO> list = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            OrderHistoryVO vo = new OrderHistoryVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
            vo.setCreatedAt(order.getCreatedAt());
            vo.setItems(getOrderItemVOs(order.getId()));
            list.add(vo);
        }

        return new PageResult<>(list,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    @Override
    @Transactional
    public void cancelOrder(Integer buyerAccountId, Integer id) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }

        OrderStatus status = order.getStatus();
        // 仅 CREATED / CONFIRMED / STOCKED 三态可取消
        if (status != OrderStatus.CREATED
                && status != OrderStatus.CONFIRMED
                && status != OrderStatus.STOCKED) {
            throw new BusinessException("当前状态不可取消");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
    }

    // ===================== 卖家端 3.12 – 3.17、3.23 =====================

    @Override
    public PageResult<OrderHistoryVO> getProductOrders(Integer productId, Integer page, Integer size) {
        List<Integer> orderIds = orderItemMapper.selectOrderIdsByProductId(productId);
        if (orderIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size);
        }

        // AB-18：同一商品的订单默认按创建时间升序，卡单再置顶
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getId, orderIds)
                .orderByAsc(Order::getCreatedAt)
                .orderByAsc(Order::getId);
        Page<Order> orderPage = page(new Page<>(page, size), wrapper);

        List<OrderHistoryVO> records = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            records.add(toSellerHistoryVO(order));
        }
        records.sort(BLOCKED_FIRST_TIME_ASC);

        return new PageResult<>(records,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    @Override
    public PageResult<OrderHistoryVO> getCustomerOrders(Integer buyerAccountId, Integer page, Integer size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerAccountId, buyerAccountId)
                .orderByDesc(Order::getCreatedAt)
                .orderByDesc(Order::getId);
        Page<Order> orderPage = page(new Page<>(page, size), wrapper);

        List<OrderHistoryVO> records = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            records.add(toSellerHistoryVO(order));
        }
        records.sort(BLOCKED_FIRST_TIME_DESC);

        return new PageResult<>(records,
                (int) orderPage.getTotal(),
                (int) orderPage.getCurrent(),
                (int) orderPage.getSize());
    }

    @Override
    @Transactional
    public void confirmOrder(Integer id) {
        requireOrder(id);
        // AB-13：CREATED → CONFIRMED，严格按序、不可跳跃
        transition(id, List.of(OrderStatus.CREATED), OrderStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void markStockDone(Integer id) {
        requireOrder(id);
        // AB-14：CONFIRMED → STOCKED，此步不校验库存，允许“库存不足仍可推进”
        transition(id, List.of(OrderStatus.CONFIRMED), OrderStatus.STOCKED);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void shipOrder(Integer id) {
        Order order = requireOrder(id);
        if (order.getStatus() != OrderStatus.STOCKED) {
            throw new BusinessException("当前状态不允许该操作");
        }

        Map<Integer, Integer> quantities = aggregateOrderQuantities(id);
        List<Integer> productIds = sortedProductIds(quantities);
        // DB-B14：对商品行加锁，缩并发窗口；加锁后再读取已发货量，保证可重复发货
        for (Integer productId : productIds) {
            if (productMapper.lockById(productId) == null) {
                throw new BusinessException("商品不存在");
            }
        }
        for (Integer productId : productIds) {
            Product product = productService.getById(productId);
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            int need = quantities.get(productId);
            int currentStock = stockOf(product);
            int shipped = shippedQuantity(productId);
            // AB-15：开始发货前，已发货量 + 本单数量 > 库存则拒绝
            if (shipped + need > currentStock) {
                throw new BusinessException("库存不足（需要 " + need + " / 当前 " + currentStock
                        + " / 已发货 " + shipped + "）");
            }
        }

        transition(id, List.of(OrderStatus.STOCKED), OrderStatus.SHIPPING);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void completeOrder(Integer id) {
        Order order = requireOrder(id);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new BusinessException("当前状态不允许该操作");
        }

        Map<Integer, Integer> quantities = aggregateOrderQuantities(id);
        List<Integer> productIds = sortedProductIds(quantities);
        for (Integer productId : productIds) {
            if (productMapper.lockById(productId) == null) {
                throw new BusinessException("商品不存在");
            }
        }

        // DB-B15：先条件更新状态，保证同一订单重复/并发点击只扣减一次
        transition(id, List.of(OrderStatus.SHIPPING), OrderStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        for (Integer productId : productIds) {
            int need = quantities.get(productId);
            Product product = productService.getById(productId);
            if (product == null) {
                throw new BusinessException("商品不存在");
            }
            int currentStock = stockOf(product);
            // AB-17：成交扣减是唯一的库存真相点，条件更新避免超卖
            if (productMapper.decreaseStockIfEnough(productId, need) == 0) {
                throw new BusinessException("库存不足：" + product.getName()
                        + "（需要 " + need + " / 当前 " + currentStock + "）");
            }
            // 成交后库存为 0 自动下架
            productMapper.autoOfflineIfStockZero(productId, now);
        }
    }

    @Override
    @Transactional
    public void cancelOrderBySeller(Integer id) {
        Order order = requireOrder(id);
        // AB-19：交易完成前均可取消
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("当前状态不可取消");
        }
        transition(id, List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED,
                OrderStatus.STOCKED, OrderStatus.SHIPPING), OrderStatus.CANCELLED);
    }

    private Order requireOrder(Integer id) {
        Order order = getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private void transition(Integer id, List<OrderStatus> fromStatuses, OrderStatus to) {
        List<String> codes = fromStatuses.stream().map(OrderStatus::getCode).toList();
        int rows = baseMapper.updateStatusFromAny(id, codes, to.getCode(), LocalDateTime.now());
        if (rows == 0) {
            throw new BusinessException("当前状态不允许该操作");
        }
    }

    private List<Integer> sortedProductIds(Map<Integer, Integer> quantities) {
        List<Integer> productIds = new ArrayList<>(quantities.keySet());
        productIds.sort(Integer::compareTo);
        return productIds;
    }

    private Map<Integer, Integer> aggregateOrderQuantities(Integer orderId) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : getOrderItems(orderId)) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private int stockOf(Product product) {
        return product.getStock() == null ? 0 : product.getStock();
    }

    private int shippedQuantity(Integer productId) {
        Integer sum = orderItemMapper.sumQuantityByProductAndStatuses(
                productId, List.of(OrderStatus.SHIPPING.getCode()));
        return sum == null ? 0 : sum;
    }

    /**
     * 卖家视角订单卡片：卡单时回填 需求/当前库存/已发货 三项证据
     */
    private OrderHistoryVO toSellerHistoryVO(Order order) {
        OrderHistoryVO vo = new OrderHistoryVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setShipBlocked(false);
        vo.setStayDays(stayDays(order));

        List<OrderItemVO> items = getOrderItemVOs(order.getId());
        vo.setItems(items);

        // AB-16：仅展示卡单证据，不做任何自动处理
        if (order.getStatus() == OrderStatus.STOCKED) {
            Map<Integer, Integer> quantities = new LinkedHashMap<>();
            for (OrderItemVO item : items) {
                quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                Product product = productService.getById(entry.getKey());
                if (product == null) {
                    continue;
                }
                int need = entry.getValue();
                int currentStock = stockOf(product);
                int shipped = shippedQuantity(entry.getKey());
                if (shipped + need > currentStock) {
                    vo.setShipBlocked(true);
                    vo.setNeedQty(need);
                    vo.setCurrentStock(currentStock);
                    vo.setShippedQty(shipped);
                    break;
                }
            }
        }
        return vo;
    }

    /**
     * 停留天数：自订单进入当前状态（updated_at）起算
     */
    private Integer stayDays(Order order) {
        LocalDateTime since = order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
        if (since == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(since.toLocalDate(), LocalDate.now());
        return (int) Math.max(days, 0);
    }

    private List<OrderItem> getOrderItems(Integer orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId);
        return orderItemMapper.selectList(wrapper);
    }

    /**
     * 解析下单明细：购物车勾选下单 / 商品详情页直接下单
     */
    private List<OrderLine> resolveOrderLines(Integer buyerAccountId, CreateOrderDTO dto) {
        List<OrderLine> lines = new ArrayList<>();

        if (dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            // 方式一：购物车勾选下单
            List<CartItem> cartItems = cartItemMapper.selectBatchIds(dto.getCartItemIds());
            for (CartItem cartItem : cartItems) {
                if (!buyerAccountId.equals(cartItem.getBuyerAccountId())) {
                    throw new BusinessException("无权访问");
                }
                lines.add(new OrderLine(cartItem.getProductId(), cartItem.getQuantity()));
            }
        } else if (dto.getProductId() != null) {
            // 方式二：商品详情页直接下单
            if (dto.getQuantity() == null || dto.getQuantity() < 1) {
                throw new BusinessException("数量必须大于0");
            }
            lines.add(new OrderLine(dto.getProductId(), dto.getQuantity()));
        }
        return lines;
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

    private List<OrderItemVO> getOrderItemVOs(Integer orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        return items.stream().map(this::toItemVO).toList();
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setProductId(item.getProductId());
        vo.setProductName(item.getProductName());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        return vo;
    }

    /** 下单明细行 */
    private record OrderLine(Integer productId, Integer quantity) {
    }
}
