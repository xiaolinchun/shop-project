package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /** 某商品关联的全部订单 ID（含已取消，只读展示） */
    @Select("SELECT DISTINCT order_id FROM order_item WHERE product_id = #{productId}")
    List<Integer> selectOrderIdsByProductId(@Param("productId") Integer productId);

    /**
     * 某商品在指定订单状态集合下的明细数量之和。
     * 在途量 = {CONFIRMED, STOCKED, SHIPPING}；已发货量 = {SHIPPING}，两者由调用方传入，禁止混用。
     */
    @Select("<script>"
            + "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi "
            + "JOIN orders o ON o.id = oi.order_id "
            + "WHERE oi.product_id = #{productId} AND o.status IN "
            + "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>#{status}</foreach>"
            + "</script>")
    Integer sumQuantityByProductAndStatuses(@Param("productId") Integer productId,
                                            @Param("statuses") List<String> statuses);

    /** 商品是否存在非终态订单（客户下单/商家确认/备货完成/开始发货） */
    @Select("SELECT COUNT(DISTINCT o.id) FROM orders o "
            + "JOIN order_item oi ON oi.order_id = o.id "
            + "WHERE oi.product_id = #{productId} "
            + "AND o.status IN ('CREATED', 'CONFIRMED', 'STOCKED', 'SHIPPING')")
    int countActiveOrdersByProductId(@Param("productId") Integer productId);
}
