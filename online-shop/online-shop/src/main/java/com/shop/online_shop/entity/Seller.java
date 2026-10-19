package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seller")
public class Seller {
    // username是数据库主键
    @TableId
    private String username;

    private String passwordHash;
}
