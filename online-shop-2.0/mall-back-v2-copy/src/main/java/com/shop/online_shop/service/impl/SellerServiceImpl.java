package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.ModifyPasswordDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.mapper.SellerMapper;
import com.shop.online_shop.service.SellerService;
import com.shop.online_shop.util.TokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SellerServiceImpl extends ServiceImpl<SellerMapper, Seller> implements SellerService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public String login(SellerLoginDTO dto) {
        Seller seller = findByUsername(dto.getUsername());
        if (seller == null || !passwordEncoder.matches(dto.getPassword(), seller.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = TokenUtil.generateToken();
        seller.setToken(token);
        updateById(seller);
        return token;
    }

    @Override
    public Seller getByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seller::getToken, token);
        return getOne(wrapper, false);
    }

    @Override
    @Transactional
    public void modifyPassword(ModifyPasswordDTO dto) {
        Seller seller = findByUsername(dto.getUsername());
        if (seller == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), seller.getPasswordHash())) {
            throw new BusinessException("旧密码错误");
        }
        seller.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        updateById(seller);
    }

    private Seller findByUsername(String username) {
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seller::getUsername, username == null ? null : username.trim());
        return getOne(wrapper, false);
    }
}
