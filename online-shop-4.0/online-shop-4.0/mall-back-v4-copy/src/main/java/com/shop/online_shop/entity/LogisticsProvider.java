package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流服务商（由卖家维护，买家端只读引用）
 */
@Data
@TableName("logistics_provider")
public class LogisticsProvider {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private LocalDateTime createdAt;
}
