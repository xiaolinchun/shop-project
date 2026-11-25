package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerPasswordDTO;
import com.shop.online_shop.entity.Seller;

public interface SellerAccountService extends IService<Seller> {

    /** 3.1 卖家登录，返回 token */
    String login(SellerLoginDTO dto);

    /** 3.2 修改密码 */
    void modifyPassword(String loginUsername, SellerPasswordDTO dto);

    /** 根据 token 获取卖家 */
    Seller getByToken(String token);
}
