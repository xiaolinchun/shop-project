package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 行锁读取商品（支付预占校验 / 确认收货扣减使用） */
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product selectByIdForUpdate(@Param("id") Integer id);

    /** 条件扣减：影响行数 0 即库存不足（不产生负库存） */
    @Update("UPDATE product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 成交扣减后库存归零 → 自动下架（offReason=STOCK_OUT） */
    @Update("UPDATE product SET status = 'OFFLINE', off_reason = 'STOCK_OUT', closed_at = NOW() "
            + "WHERE id = #{id} AND stock = 0 AND status = 'ONLINE'")
    int autoOfflineIfZero(@Param("id") Integer id);

    /** 售后退货入库：库存回补 */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{id}")
    int addStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
