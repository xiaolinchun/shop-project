package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.LogisticsProviderDTO;
import com.shop.online_shop.entity.LogisticsProvider;
import com.shop.online_shop.mapper.LogisticsProviderMapper;
import com.shop.online_shop.service.SellerLogisticsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerLogisticsServiceImpl implements SellerLogisticsService {

    @Resource
    private LogisticsProviderMapper logisticsProviderMapper;

    @Override
    public List<LogisticsProvider> list() {
        return logisticsProviderMapper.selectList(new LambdaQueryWrapper<LogisticsProvider>()
                .orderByAsc(LogisticsProvider::getId));
    }

    @Override
    @Transactional
    public Integer add(LogisticsProviderDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("物流服务商名称不能为空");
        }
        String name = dto.getName().trim();
        if (logisticsProviderMapper.selectCount(new LambdaQueryWrapper<LogisticsProvider>()
                .eq(LogisticsProvider::getName, name)) > 0) {
            throw new BusinessException("物流服务商已存在");
        }
        LogisticsProvider provider = new LogisticsProvider();
        provider.setName(name);
        provider.setCreatedAt(LocalDateTime.now());
        logisticsProviderMapper.insert(provider);
        return provider.getId();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        LogisticsProvider provider = id == null ? null : logisticsProviderMapper.selectById(id);
        if (provider == null) {
            throw new BusinessException("物流服务商不存在");
        }
        logisticsProviderMapper.deleteById(id);
    }
}
