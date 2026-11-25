package com.shop.online_shop.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址 VO
 */
@Data
public class AddressVO {

    private Integer id;
    private String name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
