package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer buyerAccountId;

    private Integer productId;

    /** 数量（1~999） */
    private Integer quantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
