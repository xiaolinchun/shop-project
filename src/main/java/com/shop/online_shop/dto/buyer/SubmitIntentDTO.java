package com.shop.online_shop.dto.buyer;

import lombok.Data;

/**
 * 提交购买意向的可选请求体。
 * <p>文档 3.7 标注“无参数”，但 v2 支持多商品同时在线，
 * 因此这里兼容前端在请求体中传 productId 的调用方式。</p>
 */
@Data
public class SubmitIntentDTO {

    private Integer productId;
}
