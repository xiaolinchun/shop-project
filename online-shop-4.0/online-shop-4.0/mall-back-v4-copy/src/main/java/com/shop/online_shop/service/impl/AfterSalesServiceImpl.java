package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.AfterSalesApplyDTO;
import com.shop.online_shop.entity.AfterSalesOrder;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.enums.AfterSalesResult;
import com.shop.online_shop.enums.AfterSalesStatus;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.mapper.AfterSalesOrderMapper;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.OrderMapper;
import com.shop.online_shop.service.AfterSalesService;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AfterSalesServiceImpl extends ServiceImpl<AfterSalesOrderMapper, AfterSalesOrder>
        implements AfterSalesService {

    private static final int MAX_IMAGES = 3;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public AfterSalesOrder apply(Integer buyerAccountId, Integer itemId, AfterSalesApplyDTO dto) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("订单明细不存在");
        }
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BusinessException("无权访问");
        }
        // 售后窗口自「开始发货」起：SHIPPING / COMPLETED
        if (order.getStatus() != OrderStatus.SHIPPING && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("当前订单状态不可发起售后");
        }
        if (hasOngoing(itemId)) {
            throw new BusinessException("该明细已有进行中的售后单");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new BusinessException("问题描述不能为空");
        }
        if (dto.getImages() != null && dto.getImages().size() > MAX_IMAGES) {
            throw new BusinessException("最多上传3张图片");
        }

        LocalDateTime now = LocalDateTime.now();
        AfterSalesOrder afterSales = new AfterSalesOrder();
        afterSales.setAfterSalesNo(generateAfterSalesNo());
        afterSales.setOrderId(order.getId());
        afterSales.setOrderItemId(itemId);
        afterSales.setBuyerAccountId(buyerAccountId);
        afterSales.setProductId(item.getProductId());
        afterSales.setProductName(item.getProductName());
        afterSales.setTitle(dto.getTitle().trim());
        afterSales.setDescription(dto.getDescription().trim());
        afterSales.setImages(dto.getImages() == null ? new ArrayList<>() : dto.getImages());
        afterSales.setStatus(AfterSalesStatus.APPLYING);
        afterSales.setCreatedAt(now);
        afterSales.setUpdatedAt(now);
        save(afterSales);
        return afterSales;
    }

    @Override
    public PageResult<AfterSalesOrderVO> listMine(Integer buyerAccountId, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSalesOrder::getBuyerAccountId, buyerAccountId)
                .orderByDesc(AfterSalesOrder::getCreatedAt)
                .orderByDesc(AfterSalesOrder::getId);
        Page<AfterSalesOrder> afterSalesPage = page(new Page<>(pageNo, pageSize), wrapper);

        List<AfterSalesOrderVO> list = new ArrayList<>();
        for (AfterSalesOrder afterSales : afterSalesPage.getRecords()) {
            Order order = orderMapper.selectById(afterSales.getOrderId());
            AfterSalesOrderVO vo = new AfterSalesOrderVO();
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
            list.add(vo);
        }
        return new PageResult<>(list, (int) afterSalesPage.getTotal(),
                (int) afterSalesPage.getCurrent(), (int) afterSalesPage.getSize());
    }

    @Override
    public Set<Integer> getOngoingItemIds(Collection<Integer> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AfterSalesOrder::getOrderItemId)
                .in(AfterSalesOrder::getOrderItemId, orderItemIds)
                .ne(AfterSalesOrder::getStatus, AfterSalesStatus.CLOSED);
        Set<Integer> result = new HashSet<>();
        for (AfterSalesOrder afterSales : list(wrapper)) {
            result.add(afterSales.getOrderItemId());
        }
        return result;
    }

    @Override
    public Set<Integer> getRefundClosedItemIds(Collection<Integer> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AfterSalesOrder::getOrderItemId)
                .in(AfterSalesOrder::getOrderItemId, orderItemIds)
                .eq(AfterSalesOrder::getStatus, AfterSalesStatus.CLOSED)
                .in(AfterSalesOrder::getResult,
                        AfterSalesResult.REFUND_AND_RETURN, AfterSalesResult.REFUND_ONLY);
        Set<Integer> result = new HashSet<>();
        for (AfterSalesOrder afterSales : list(wrapper)) {
            result.add(afterSales.getOrderItemId());
        }
        return result;
    }

    private boolean hasOngoing(Integer itemId) {
        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSalesOrder::getOrderItemId, itemId)
                .ne(AfterSalesOrder::getStatus, AfterSalesStatus.CLOSED);
        return count(wrapper) > 0;
    }

    private String generateAfterSalesNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<AfterSalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(AfterSalesOrder::getAfterSalesNo, "AS" + datePart);
        long count = count(wrapper);
        return "AS" + datePart + String.format("%04d", count + 1);
    }
}
