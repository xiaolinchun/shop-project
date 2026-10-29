package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.ModifyPasswordDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.entity.Seller;

public interface SellerService extends IService<Seller> {

    String login(SellerLoginDTO dto);

    Seller getByToken(String token);

    void modifyPassword(ModifyPasswordDTO dto);
}
