package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.PaymentChannel;
import com.shop.online_shop.vo.AddressSnapshotVO;
import com.shop.online_shop.vo.OrderItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 订单号（日期 + 4 位流水） */
    private String orderNo;

    private OrderStatus status;

    /** 总金额 = Σ(明细快照价 × 数量)，无运费 */
    private BigDecimal totalAmount;

    private Integer buyerAccountId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ===================== v4.0 支付字段 =====================

    /** 支付渠道，未支付为 null */
    private PaymentChannel paymentChannel;

    /** 支付成功时间，未支付为 null */
    private LocalDateTime paidAt;

    // ===================== v4.0 收货地址快照（下单后不可改） =====================

    private String addressName;

    private String addressPhone;

    private String addressProvince;

    private String addressCity;

    private String addressDistrict;

    private String addressDetail;

    // ===================== v4.0 物流字段（一单一运单） =====================

    /** 物流服务商名称快照 */
    private String logisticsProvider;

    private String waybillNo;

    // ===================== 计算字段 =====================

    /** 买家用户名（计算字段） */
    @TableField(exist = false)
    private String buyerUsername;

    /** 商品清单（行内展开） */
    @TableField(exist = false)
    private List<OrderItemVO> items;

    /** 收货地址快照（由 address 六字段组装） */
    @TableField(exist = false)
    private AddressSnapshotVO addressSnapshot;
}
