package com.shop.online_shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buyer_account")
public class BuyerAccount {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    @JsonIgnore
    private String passwordHash;

    private String phone;

    /** 默认交易地点，账号级选填项，不入订单 */
    private String defaultTradeLocation;

    @JsonIgnore
    private String token;

    private LocalDateTime createdAt;
}
