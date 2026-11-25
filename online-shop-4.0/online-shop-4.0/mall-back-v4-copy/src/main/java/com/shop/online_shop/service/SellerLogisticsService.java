package com.shop.online_shop.service;

import com.shop.online_shop.dto.seller.LogisticsProviderDTO;
import com.shop.online_shop.entity.LogisticsProvider;

import java.util.List;

public interface SellerLogisticsService {

    /** 3.25 物流服务商列表 */
    List<LogisticsProvider> list();

    /** 3.26 新增物流服务商 */
    Integer add(LogisticsProviderDTO dto);

    /** 3.27 删除物流服务商 */
    void delete(Integer id);
}
