package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shop.online_shop.enums.AfterSalesResult;
import com.shop.online_shop.enums.AfterSalesStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单（挂在订单明细上的平行单据，不改写订单状态）
 */
@Data
@TableName(value = "after_sales_order", autoResultMap = true)
public class AfterSalesOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String afterSalesNo;

    private Integer orderId;

    private Integer orderItemId;

    private Integer buyerAccountId;

    private Integer productId;

    /** 商品名快照 */
    private String productName;

    private String title;

    private String description;

    @TableField(value = "images", typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private AfterSalesStatus status;

    private AfterSalesResult result;

    private LocalDateTime resultAt;

    private String returnWaybillNo;

    private String exchangeWaybillNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
