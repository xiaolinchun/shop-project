package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerPasswordDTO;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.mapper.SellerMapper;
import com.shop.online_shop.service.SellerAccountService;
import com.shop.online_shop.util.TokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SellerAccountServiceImpl extends ServiceImpl<SellerMapper, Seller> implements SellerAccountService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public String login(SellerLoginDTO dto) {
        Seller seller = findByUsername(dto.getUsername());
        if (seller == null || !passwordEncoder.matches(dto.getPassword(), seller.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        seller.setToken(TokenUtil.generateToken());
        updateById(seller);
        return seller.getToken();
    }

    @Override
    @Transactional
    public void modifyPassword(String loginUsername, SellerPasswordDTO dto) {
        if (StringUtils.hasText(dto.getUsername())
                && !dto.getUsername().trim().equals(loginUsername)) {
            throw new BusinessException("无权访问");
        }
        Seller seller = getById(loginUsername);
        if (seller == null) {
            throw new BusinessException("卖家不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), seller.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("新密码不少于8位");
        }
        seller.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        updateById(seller);
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

    private Seller findByUsername(String username) {
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seller::getUsername, username == null ? null : username.trim());
        return getOne(wrapper, false);
    }
}
