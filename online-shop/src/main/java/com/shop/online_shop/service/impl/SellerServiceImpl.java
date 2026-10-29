package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.ModifyPwdDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.mapper.SellerMapper;
import com.shop.online_shop.service.SellerService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SellerServiceImpl extends ServiceImpl<SellerMapper, Seller> implements SellerService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public boolean login(SellerLoginDTO dto) {
        System.out.println("====登录调试====");
        System.out.println("前端传入用户名：" + dto.getUsername());
        System.out.println("前端传入密码：" + dto.getPassword());

        Seller seller = getById(dto.getUsername());
        if(seller == null){
            System.out.println("数据库没有查到该账号！seller=null");
            throw new BusinessException("账号不存在");
        }
        System.out.println("数据库查到seller对象，username=" + seller.getUsername());
        System.out.println("数据库读取的passwordHash = " + seller.getPasswordHash());

        boolean matchResult = encoder.matches(dto.getPassword(), seller.getPasswordHash());
        System.out.println("密码比对结果：" + matchResult);
        return matchResult;
    }

    @Override
    public void changePassword(ModifyPwdDTO dto) {
        Seller seller = getById(dto.getUsername());
        if(seller == null){
            throw new BusinessException("账号不存在");
        }
        if(!encoder.matches(dto.getOldPassword(),seller.getPasswordHash())){
            throw new BusinessException("原密码错误");
        }
        seller.setPasswordHash(encoder.encode(dto.getNewPassword()));
        updateById(seller);
    }
}