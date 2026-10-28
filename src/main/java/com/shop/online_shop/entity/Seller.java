package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
@TableName("seller")
public class Seller {

    @TableId(value = "username", type = IdType.INPUT)
    private String username;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private String token;
}
