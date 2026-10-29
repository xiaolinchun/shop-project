package com.shop.online_shop.dto.buyer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BuyerLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
