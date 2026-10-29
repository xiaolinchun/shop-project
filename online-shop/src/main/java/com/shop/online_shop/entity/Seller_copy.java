package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seller_copy")
public class Seller_copy{
    private String username;
    private String passwordHash;
}