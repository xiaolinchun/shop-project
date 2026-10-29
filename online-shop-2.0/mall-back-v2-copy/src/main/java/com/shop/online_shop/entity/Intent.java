package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.online_shop.enums.IntentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("intent")
public class Intent {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String buyerName;

    private String phone;

    private IntentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime resultAt;

    private Integer productId;

    private String productName;

    private Integer buyerAccountId;

    @TableField(exist = false)
    private String buyerUsername;
}
