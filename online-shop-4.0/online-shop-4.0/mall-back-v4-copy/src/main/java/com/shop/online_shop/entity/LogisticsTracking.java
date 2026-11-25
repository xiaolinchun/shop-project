package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.TrackingStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流跟踪条目（揽收 / 运输中 / 派送中 / 已签收 + 时间；仅展示，不推进订单状态）
 */
@Data
@TableName("logistics_tracking")
public class LogisticsTracking {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private String waybillNo;

    private TrackingStatus status;

    private String description;

    private LocalDateTime trackedAt;

    private LocalDateTime createdAt;
}
