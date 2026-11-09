package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 3.1 卖家登录请求
 */
@Data
public class SellerLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
