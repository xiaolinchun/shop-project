package com.shop.online_shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.online_shop.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Select("SELECT * FROM payment_record WHERE payment_no = #{paymentNo} FOR UPDATE")
    PaymentRecord selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /** 同一订单进行中 / 已成功的有效支付 */
    @Select("SELECT * FROM payment_record WHERE order_id = #{orderId} "
            + "AND status IN ('PROCESSING','SUCCESS') ORDER BY id DESC LIMIT 1")
    PaymentRecord selectActiveByOrderId(@Param("orderId") Integer orderId);

    @Select("SELECT * FROM payment_record WHERE order_id = #{orderId} "
            + "AND status = 'SUCCESS' ORDER BY id DESC LIMIT 1")
    PaymentRecord selectSuccessByOrderId(@Param("orderId") Integer orderId);

    /** 已退款的支付单（同单多售后兜底退款使用） */
    @Select("SELECT * FROM payment_record WHERE order_id = #{orderId} "
            + "AND status = 'REFUNDED' ORDER BY id DESC LIMIT 1")
    PaymentRecord selectRefundedByOrderId(@Param("orderId") Integer orderId);
}
