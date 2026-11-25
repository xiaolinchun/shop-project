package com.shop.online_shop.dto.buyer;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 购物车批量转收藏请求
 */
@Data
public class CartToFavoriteDTO {

    @NotEmpty(message = "请至少选择一条记录")
    private List<Integer> cartItemIds;
}
