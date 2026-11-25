package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.MockChannelTxn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MockChannelTxnMapper extends BaseMapper<MockChannelTxn> {

    @Select("SELECT * FROM mock_channel_txn WHERE payment_no = #{paymentNo} ORDER BY id DESC LIMIT 1")
    MockChannelTxn selectLatestByPaymentNo(@Param("paymentNo") String paymentNo);
}
