package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.OrderItem;
import com.shop.online_shop.vo.ProductReservedRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 预占量 = 所有「已支付且未成交」订单（PAID / CONFIRMED / STOCKED / SHIPPING）的明细数量之和
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi "
            + "JOIN orders o ON oi.order_id = o.id "
            + "WHERE oi.product_id = #{productId} "
            + "AND o.status IN ('PAID','CONFIRMED','STOCKED','SHIPPING')")
    int sumReservedByProductId(@Param("productId") Integer productId);

    @Select("<script>"
            + "SELECT oi.product_id AS productId, SUM(oi.quantity) AS quantity "
            + "FROM order_item oi JOIN orders o ON oi.order_id = o.id "
            + "WHERE o.status IN ('PAID','CONFIRMED','STOCKED','SHIPPING') "
            + "AND oi.product_id IN "
            + "<foreach collection='productIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach> "
            + "GROUP BY oi.product_id"
            + "</script>")
    List<ProductReservedRow> sumReservedByProductIds(@Param("productIds") List<Integer> productIds);

    /**
     * 已发货量（展示口径）：已发货 / 已成交订单的明细数量之和
     */
    @Select("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi "
            + "JOIN orders o ON oi.order_id = o.id "
            + "WHERE oi.product_id = #{productId} "
            + "AND o.status IN ('SHIPPING','COMPLETED')")
    int sumShippedByProductId(@Param("productId") Integer productId);
}
