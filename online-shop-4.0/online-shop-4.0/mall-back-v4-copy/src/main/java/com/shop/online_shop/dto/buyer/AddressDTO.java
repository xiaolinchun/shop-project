package com.shop.online_shop.dto.buyer;

import lombok.Data;

/**
 * 收货地址新增 / 编辑请求
 */
@Data
public class AddressDTO {

    private String name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Boolean isDefault;
}
