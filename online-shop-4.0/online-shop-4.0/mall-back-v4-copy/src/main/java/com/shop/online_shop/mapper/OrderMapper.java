package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 行锁读取订单（支付 / 取消 / 确认收货等关键交易使用） */
    @Select("SELECT * FROM orders WHERE id = #{id} FOR UPDATE")
    Order selectByIdForUpdate(@Param("id") Integer id);
}
