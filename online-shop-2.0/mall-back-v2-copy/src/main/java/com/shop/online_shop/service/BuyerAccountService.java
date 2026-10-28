package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.PageResult;

public interface BuyerAccountService extends IService<BuyerAccount> {

    String register(BuyerRegisterDTO dto);

    String login(BuyerLoginDTO dto);

    BuyerAccount getByToken(String token);

    PageResult<CustomerVO> listCustomers(Integer page, Integer size);

    BuyerAccount getByUsername(String username);
}
