package com.shop.online_shop.service.impl;

import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.service.ReservedQuantityService;
import com.shop.online_shop.vo.ProductReservedRow;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservedQuantityServiceImpl implements ReservedQuantityService {

    @Resource
    private OrderItemMapper orderItemMapper;

    @Override
    public int getReservedQuantity(Integer productId) {
        if (productId == null) {
            return 0;
        }
        return orderItemMapper.sumReservedByProductId(productId);
    }

    @Override
    public Map<Integer, Integer> getReservedQuantities(Collection<Integer> productIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        List<ProductReservedRow> rows = orderItemMapper.sumReservedByProductIds(List.copyOf(productIds));
        for (ProductReservedRow row : rows) {
            result.put(row.getProductId(), row.getQuantity() == null ? 0 : row.getQuantity());
        }
        return result;
    }

    @Override
    public int getShippedQuantity(Integer productId) {
        if (productId == null) {
            return 0;
        }
        return orderItemMapper.sumShippedByProductId(productId);
    }
}
