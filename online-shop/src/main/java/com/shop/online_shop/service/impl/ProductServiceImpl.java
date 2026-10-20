package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.IntentStatusEnum;
import com.shop.online_shop.enums.ProductStatusEnum;
import com.shop.online_shop.mapper.IntentMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Resource
    private IntentMapper intentMapper;
    @Resource
    private IntentService intentService;

    @Override
    public Product getCurrentActiveProduct() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Product::getStatus, ProductStatusEnum.ONLINE, ProductStatusEnum.FROZEN);
        wrapper.last("LIMIT 1");
        return getOne(wrapper, false);
    }

    @Override
    @Transactional
    public void publish(PublishProductDTO dto) {
        Product active = getCurrentActiveProduct();
        if(active != null){
            throw new BusinessException("已有在售/冻结商品，不能发布新商品，请先下架");
        }
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setPrice(dto.getPrice());
        product.setStatus(ProductStatusEnum.ONLINE);
        product.setPublishedAt(LocalDateTime.now());
        product.setSellerId("seller01");
        save(product);
        //发布之后触发自动排队判断
        intentService.autoTradeAndFreeze(product.getId());
    }

    @Override
    @Transactional
    public void manualFreeze(Integer productId) {
        Product product = getById(productId);
        if(product == null){
            throw new BusinessException("商品不存在");
        }
        if(!ProductStatusEnum.ONLINE.equals(product.getStatus())){
            throw new BusinessException("只有在售商品才可以手动冻结");
        }
        product.setStatus(ProductStatusEnum.FROZEN);
        updateById(product);
    }

    @Override
    @Transactional
    public void manualUnFreeze(Integer productId) {
        Product product = getById(productId);
        if(product == null){
            throw new BusinessException("商品不存在");
        }
        if(!ProductStatusEnum.FROZEN.equals(product.getStatus())){
            throw new BusinessException("商品未冻结，无需解冻");
        }
        // 判断是否存在交易中的意向，如果存在，禁止手动解冻（SRS：交易中禁止直接解冻）
        LambdaQueryWrapper<Intent> intentWrapper = new LambdaQueryWrapper<>();
        intentWrapper.eq(Intent::getProductId,productId)
                .eq(Intent::getStatus, IntentStatusEnum.TRADING);
        Long tradingCount = intentMapper.selectCount(intentWrapper);
        if(tradingCount > 0){
            throw new BusinessException("当前存在正在交易的意向，禁止手动解冻，请先标记交易结果");
        }
        product.setStatus(ProductStatusEnum.ONLINE);
        updateById(product);
        intentService.autoTradeAndFreeze(productId);
    }

    @Override
    @Transactional
    
public void manualOffShelf(Integer productId) {
    Product product = getById(productId);
    if(product == null){
        throw new BusinessException("商品不存在");
    }
    if(ProductStatusEnum.OFFLINE.equals(product.getStatus())){
        throw new BusinessException("商品已经下架");
    }
    product.setStatus(ProductStatusEnum.OFFLINE);
    product.setClosedAt(LocalDateTime.now());
    updateById(product);

    LambdaQueryWrapper<Intent> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(Intent::getProductId, productId);
    List<Intent> list = intentMapper.selectList(wrapper);

    for (Intent intent : list) {
        if(IntentStatusEnum.SUCCESS.equals(intent.getStatus())
        || IntentStatusEnum.TRADING.equals(intent.getStatus())){
            intent.setStatus(IntentStatusEnum.FAILED);
            intent.setUpdatedAt(LocalDateTime.now());
            intentMapper.updateById(intent);
        }
    }
}

}