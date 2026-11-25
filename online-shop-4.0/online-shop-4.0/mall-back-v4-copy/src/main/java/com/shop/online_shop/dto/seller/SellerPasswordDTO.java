package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 卖家修改密码请求
 */
@Data
public class SellerPasswordDTO {

    private String username;

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
