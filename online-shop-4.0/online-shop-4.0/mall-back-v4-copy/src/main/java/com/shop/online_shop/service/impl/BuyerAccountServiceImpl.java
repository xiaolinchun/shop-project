package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.mapper.BuyerAccountMapper;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.util.TokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class BuyerAccountServiceImpl extends ServiceImpl<BuyerAccountMapper, BuyerAccount>
        implements BuyerAccountService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public String register(BuyerRegisterDTO dto) {
        BuyerAccount existing = findByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        BuyerAccount account = new BuyerAccount();
        account.setUsername(dto.getUsername().trim());
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setPhone(dto.getPhone());
        account.setDefaultTradeLocation(StringUtils.hasText(dto.getDefaultTradeLocation())
                ? dto.getDefaultTradeLocation()
                : null);
        account.setToken(TokenUtil.generateToken());
        account.setCreatedAt(LocalDateTime.now());
        save(account);
        return account.getToken();
    }

    @Override
    @Transactional
    public String login(BuyerLoginDTO dto) {
        BuyerAccount account = findByUsername(dto.getUsername());
        if (account == null || !passwordEncoder.matches(dto.getPassword(), account.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        account.setToken(TokenUtil.generateToken());
        updateById(account);
        return account.getToken();
    }

    @Override
    public BuyerAccount getByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        LambdaQueryWrapper<BuyerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerAccount::getToken, token);
        return getOne(wrapper, false);
    }

    @Override
    public BuyerAccount getByUsername(String username) {
        return findByUsername(username);
    }

    private BuyerAccount findByUsername(String username) {
        LambdaQueryWrapper<BuyerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuyerAccount::getUsername, username == null ? null : username.trim());
        return getOne(wrapper, false);
    }
}
