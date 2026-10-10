package com.shop.online_shop.dto.seller;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SellerLoginDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
