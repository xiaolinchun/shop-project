package com.shop.online_shop.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货地址快照（下单时写入订单，后续地址簿变更不回写）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshotVO {

    private String name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
}
