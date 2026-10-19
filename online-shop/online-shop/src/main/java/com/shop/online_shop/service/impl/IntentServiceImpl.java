package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.buyer.IntentQueryVO;
import com.shop.online_shop.dto.buyer.ModifyIntentDTO;
import com.shop.online_shop.dto.buyer.SubmitIntentDTO;
import com.shop.online_shop.dto.seller.TradeMarkDTO;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.IntentStatusEnum;
import com.shop.online_shop.enums.ProductStatusEnum;
import com.shop.online_shop.mapper.IntentMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.util.TokenUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IntentServiceImpl extends ServiceImpl<IntentMapper, Intent> implements IntentService {

    @Resource
    private IntentMapper intentMapper;
    @Resource
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void autoTradeAndFreeze(Integer productId) {
        Product product = productMapper.selectById(productId);
        if(product == null) return;
        if(!ProductStatusEnum.ONLINE.equals(product.getStatus())){
            return;
        }
        //查询排队队列，按提交时间升序
        LambdaQueryWrapper<Intent> qw = new LambdaQueryWrapper<>();
        qw.eq(Intent::getProductId,productId)
                .eq(Intent::getStatus, IntentStatusEnum.QUEUED)
                .orderByAsc(Intent::getCreatedAt);
        List<Intent> queue = intentMapper.selectList(qw);
        if(queue.isEmpty()){
            return;
        }
        Intent head = queue.get(0);
        //队首变为交易中
        head.setStatus(IntentStatusEnum.TRADING);
        head.setUpdatedAt(LocalDateTime.now());
        intentMapper.updateById(head);
        //商品冻结
        product.setStatus(ProductStatusEnum.FROZEN);
        productMapper.updateById(product);
    }

    @Override
    @Transactional
    public String submitIntent(SubmitIntentDTO dto) {
        Product activeProduct = null;
        LambdaQueryWrapper<Product> prodWrap = new LambdaQueryWrapper<>();
        prodWrap.in(Product::getStatus,ProductStatusEnum.ONLINE,ProductStatusEnum.FROZEN);
        //原来注释掉的一行
        // activeProduct = productMapper.selectOne(prodWrap);
        // 1. 先查列表
        List<Product> productList = productMapper.selectList(prodWrap);
        if (productList == null || productList.isEmpty()) {
            throw new BusinessException("当前没有在售商品");
        }
        // 取第一条在售商品
        activeProduct = productList.get(0);

        

        if(activeProduct == null){
            throw new BusinessException("暂无在售商品");
        }
        if(ProductStatusEnum.FROZEN.equals(activeProduct.getStatus())){
            throw new BusinessException("商品交易中，不能提交意向");
        }
        Intent intent = new Intent();
        intent.setBuyerName(dto.getBuyerName());
        intent.setPhone(dto.getPhone());
        intent.setToken(TokenUtil.generateToken());
        intent.setStatus(IntentStatusEnum.QUEUED);
        LocalDateTime now = LocalDateTime.now();
        intent.setCreatedAt(now);
        intent.setUpdatedAt(now);
        intent.setProductId(activeProduct.getId());
        save(intent);
        //提交意向后执行自动交易判断
        autoTradeAndFreeze(activeProduct.getId());
        return intent.getToken();
    }

    @Override
public IntentQueryVO queryByToken(String token) {
    LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Intent::getToken,token);
    Intent intent = getOne(wrapper);
    if(intent == null){
        throw new BusinessException("口令码无效或者已经失效");
    }
    IntentQueryVO vo = new IntentQueryVO();
    vo.setIntentId(intent.getId());
    vo.setStatus(intent.getStatus());
    if(IntentStatusEnum.QUEUED.equals(intent.getStatus())){
        //仅排队中才统计前面人数
        LambdaQueryWrapper<Intent> queueWrap = new LambdaQueryWrapper<>();
        queueWrap.eq(Intent::getProductId,intent.getProductId())
                .eq(Intent::getStatus, IntentStatusEnum.QUEUED)
                .lt(Intent::getCreatedAt, intent.getCreatedAt());
        long ahead = intentMapper.selectCount(queueWrap);
        vo.setQueueIndex((int)ahead);
    }else if(IntentStatusEnum.TRADING.equals(intent.getStatus())){
        vo.setQueueIndex(0); //交易中，前面0人
    }else{
        vo.setQueueIndex(null); //终态，无排队位次
    }
    return vo;
}


    @Override
    @Transactional
    public void modifyByToken(ModifyIntentDTO dto) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getToken, dto.getToken());
        Intent intent = getOne(wrapper);
        if(intent == null){
            throw new BusinessException("口令码无效或者已经失效");
        }
        //只修改姓名电话，不改变排队时间、位次
        intent.setBuyerName(dto.getBuyerName());
        intent.setPhone(dto.getPhone());
        intent.setUpdatedAt(LocalDateTime.now());
        updateById(intent);
    }

    @Override
    @Transactional
    public void cancelByToken(String token) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getToken,token);
        Intent intent = getOne(wrapper);
        if(intent == null){
            throw new BusinessException("口令码无效或者已经失效");
        }

        // =========新增状态校验=========
        if(!IntentStatusEnum.QUEUED.equals(intent.getStatus())
                && !IntentStatusEnum.TRADING.equals(intent.getStatus())){
            throw new BusinessException("该意向已经终态，不能撤销");
        }

        //设置撤销终态
        intent.setStatus(IntentStatusEnum.CANCELLED);
        intent.setUpdatedAt(LocalDateTime.now());
        updateById(intent);
        //撤销后自动递补
        autoTradeAndFreeze(intent.getProductId());
    }

    @Override
    @Transactional
    public void markTradeResult(TradeMarkDTO dto) {
        Intent intent = getById(dto.getIntentId());
        if(intent == null){
            throw new BusinessException("意向不存在");
        }
        Integer productId = intent.getProductId();
        Product product = productMapper.selectById(productId);
        if(product == null){
            throw new BusinessException("商品不存在");
        }
        if(!IntentStatusEnum.TRADING.equals(intent.getStatus())){
            throw new BusinessException("只有交易中的意向才允许标记交易结果");
        }
        String op = dto.getOperate();
        if("SUCCESS".equals(op)){
            //FR‑09 标记交易成功
            intent.setStatus(IntentStatusEnum.SUCCESS);
            intent.setUpdatedAt(LocalDateTime.now());
            updateById(intent);
            //商品直接冻结→下架
            product.setStatus(ProductStatusEnum.OFFLINE);
            product.setClosedAt(LocalDateTime.now());
            productMapper.updateById(product);
            //该商品其余所有意向置失败
            LambdaQueryWrapper<Intent> restWrapper = new LambdaQueryWrapper<>();
            restWrapper.eq(Intent::getProductId,productId)
                    .in(Intent::getStatus, IntentStatusEnum.QUEUED, IntentStatusEnum.TRADING);
            List<Intent> restList = intentMapper.selectList(restWrapper);
            for(Intent i : restList){
                i.setStatus(IntentStatusEnum.FAILED);
                i.setUpdatedAt(LocalDateTime.now());
                intentMapper.updateById(i);
            }
        }else if("FAILURE".equals(op)){
            //作废，意向失败
            intent.setStatus(IntentStatusEnum.FAILED);
            intent.setUpdatedAt(LocalDateTime.now());
            updateById(intent);
            //商品回到在售
            product.setStatus(ProductStatusEnum.ONLINE);
            productMapper.updateById(product);
            autoTradeAndFreeze(productId);
        }else if("RE_QUEUE".equals(op)){
            //重新排队：回到QUEUED，刷新created_at排到队尾，口令码不变
            intent.setStatus(IntentStatusEnum.QUEUED);
            intent.setCreatedAt(LocalDateTime.now());
            intent.setUpdatedAt(LocalDateTime.now());
            updateById(intent);
            product.setStatus(ProductStatusEnum.ONLINE);
            productMapper.updateById(product);
            autoTradeAndFreeze(productId);
        }else{
            throw new BusinessException("非法操作类型");
        }
    }
}