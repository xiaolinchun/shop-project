package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 行级锁：用于发货前容量校验时缩小并发窗口 */
    @Select("SELECT id FROM product WHERE id = #{id} FOR UPDATE")
    Integer lockById(@Param("id") Integer id);

    /** 条件更新扣减库存：仅 stock >= quantity 时扣减，返回 0 表示不足 */
    @Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStockIfEnough(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 成交扣减后库存为 0 自动下架（不受“存在非终态订单”限制） */
    @Update("UPDATE product SET status = 'OFFLINE', off_reason = 'STOCK_OUT', closed_at = #{now} "
            + "WHERE id = #{id} AND stock = 0 AND status = 'ONLINE'")
    int autoOfflineIfStockZero(@Param("id") Integer id, @Param("now") LocalDateTime now);
}
