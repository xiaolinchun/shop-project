package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址簿（每账号至多 1 个默认地址）
 */
@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer buyerAccountId;

    private String name;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    /** 是否默认地址 */
    private Boolean isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
