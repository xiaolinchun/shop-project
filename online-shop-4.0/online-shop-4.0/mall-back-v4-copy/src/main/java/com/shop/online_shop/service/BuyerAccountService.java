package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.entity.BuyerAccount;

public interface BuyerAccountService extends IService<BuyerAccount> {

    /** 2.5 买家注册，返回 token */
    String register(BuyerRegisterDTO dto);

    /** 2.6 买家登录，返回 token */
    String login(BuyerLoginDTO dto);

    /** 根据 token 获取买家账号 */
    BuyerAccount getByToken(String token);

    BuyerAccount getByUsername(String username);
}
