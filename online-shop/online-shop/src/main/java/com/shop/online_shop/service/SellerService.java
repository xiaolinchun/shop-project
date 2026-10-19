package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.ModifyPwdDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.entity.Seller;

public interface SellerService extends IService<Seller> {
    boolean login(SellerLoginDTO dto);
    void changePassword(ModifyPwdDTO dto);
}