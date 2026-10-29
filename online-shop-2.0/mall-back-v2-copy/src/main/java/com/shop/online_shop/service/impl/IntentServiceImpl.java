package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.IntentStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.BuyerAccountMapper;
import com.shop.online_shop.mapper.IntentMapper;
import com.shop.online_shop.mapper.ProductMapper;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.vo.IntentHistoryVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IntentServiceImpl extends ServiceImpl<IntentMapper, Intent> implements IntentService {

    private static final String OPERATE_START = "START";
    private static final String OPERATE_SUCCESS = "SUCCESS";
    private static final String OPERATE_FAIL = "FAILURE";
    private static final String OPERATE_RE_QUEUE = "RE_QUEUE";

    @Resource
    private ProductMapper productMapper;

    @Resource
    private BuyerAccountMapper buyerAccountMapper;

    @Override
    @Transactional
    public Integer submit(Integer productId, Integer buyerAccountId) {
        Product product;
        if (productId != null) {
            product = productMapper.selectById(productId);
        } else {
            product = findLatestOnlineProduct();
        }

        if (product == null || ProductStatus.ONLINE != product.getStatus()) {
            throw new BusinessException("商品当前不接收意向");
        }

        LocalDateTime now = LocalDateTime.now();
        Intent intent = new Intent();
        intent.setBuyerName(null);
        intent.setPhone(null);
        intent.setStatus(IntentStatus.QUEUED);
        intent.setCreatedAt(now);
        intent.setUpdatedAt(now);
        intent.setProductId(product.getId());
        intent.setProductName(product.getName());
        intent.setBuyerAccountId(buyerAccountId);
        save(intent);
        return intent.getId();
    }

    @Override
    public PageResult<IntentHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getBuyerAccountId, buyerAccountId)
                .in(Intent::getStatus, IntentStatus.SUCCESS, IntentStatus.FAILED)
                .orderByDesc(Intent::getCreatedAt)
                .orderByDesc(Intent::getId);

        Page<Intent> intentPage = page(new Page<>(page, size), wrapper);
        List<IntentHistoryVO> list = intentPage.getRecords().stream()
                .map(this::toHistoryVO)
                .toList();

        return new PageResult<>(list,
                (int) intentPage.getTotal(),
                (int) intentPage.getCurrent(),
                (int) intentPage.getSize());
    }

    @Override
    public List<Intent> listQueueByProduct(Integer productId, String sellerId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !sellerId.equals(product.getSellerId())) {
            throw new BusinessException("无权限操作该商品");
        }

        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId, productId)
                .in(Intent::getStatus, IntentStatus.QUEUED, IntentStatus.TRADING)
                .orderByAsc(Intent::getCreatedAt)
                .orderByAsc(Intent::getId);

        List<Intent> intents = list(wrapper);
        fillBuyerUsernames(intents);
        return intents;
    }

    @Override
    @Transactional
    public void markTrade(Integer intentId, String operate, String sellerId) {
        Intent intent = getById(intentId);
        if (intent == null) {
            throw new BusinessException("意向不存在");
        }

        Product product = productMapper.selectById(intent.getProductId());
        if (product == null || !sellerId.equals(product.getSellerId())) {
            throw new BusinessException("无权限操作该意向");
        }

        LocalDateTime now = LocalDateTime.now();

        switch (operate) {
            case OPERATE_START -> {
                if (IntentStatus.TRADING == intent.getStatus()) {
                    throw new BusinessException("该意向已在交易中");
                }
                if (IntentStatus.QUEUED != intent.getStatus()) {
                    throw new BusinessException("只能对排队中的意向发起交易");
                }
                if (!isHeadOfQueue(intent)) {
                    throw new BusinessException("只能与队首买家进行交易");
                }
                if (hasTradingIntent(intent.getProductId())) {
                    throw new BusinessException("该商品已有交易在进行中");
                }
                intent.setStatus(IntentStatus.TRADING);
                intent.setUpdatedAt(now);
                updateById(intent);
                // RA2：进入交易后商品自动冻结
                if (ProductStatus.ONLINE == product.getStatus()) {
                    product.setStatus(ProductStatus.FROZEN);
                    productMapper.updateById(product);
                }
            }

            case OPERATE_SUCCESS -> {
                if (IntentStatus.TRADING != intent.getStatus()) {
                    throw new BusinessException("只能对交易中的意向标记成功");
                }
                intent.setStatus(IntentStatus.SUCCESS);
                intent.setUpdatedAt(now);
                intent.setResultAt(now);
                updateById(intent);

                product.setStock(product.getStock() - 1);
                if (product.getStock() <= 0) {
                    // RA4：库存归零自动下架
                    product.setStock(0);
                    product.setStatus(ProductStatus.OFFLINE);
                    product.setClosedAt(now);
                } else if (ProductStatus.FROZEN == product.getStatus()) {
                    // 仍有库存：解除交易冻结，恢复在售
                    product.setStatus(ProductStatus.ONLINE);
                }
                productMapper.updateById(product);
            }

            case OPERATE_FAIL -> {
                if (IntentStatus.TRADING != intent.getStatus()) {
                    throw new BusinessException("只能对交易中的意向标记失败");
                }
                intent.setStatus(IntentStatus.FAILED);
                intent.setUpdatedAt(now);
                intent.setResultAt(now);
                updateById(intent);
                // 交易由 START 发起，失败后解冻回在售
                if (ProductStatus.FROZEN == product.getStatus()) {
                    product.setStatus(ProductStatus.ONLINE);
                    productMapper.updateById(product);
                }
            }

            case OPERATE_RE_QUEUE -> {
                if (IntentStatus.TRADING != intent.getStatus()) {
                    throw new BusinessException("只能对交易中的意向重新排队");
                }
                intent.setStatus(IntentStatus.QUEUED);
                intent.setCreatedAt(now);
                intent.setUpdatedAt(now);
                updateById(intent);
                if (ProductStatus.FROZEN == product.getStatus()) {
                    product.setStatus(ProductStatus.ONLINE);
                    productMapper.updateById(product);
                }
            }

            default -> throw new BusinessException("无效的操作类型");
        }
    }

    @Override
    public PageResult<IntentHistoryVO> getCustomerHistory(Integer buyerAccountId, Integer page, Integer size) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getBuyerAccountId, buyerAccountId)
                .in(Intent::getStatus, IntentStatus.SUCCESS, IntentStatus.FAILED)
                .orderByDesc(Intent::getCreatedAt)
                .orderByDesc(Intent::getId);

        Page<Intent> intentPage = page(new Page<>(page, size), wrapper);
        List<IntentHistoryVO> list = intentPage.getRecords().stream()
                .map(this::toHistoryVO)
                .toList();

        return new PageResult<>(list,
                (int) intentPage.getTotal(),
                (int) intentPage.getCurrent(),
                (int) intentPage.getSize());
    }

    @Override
    public PageResult<Intent> listAnonymousIntent(Integer page, Integer size) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Intent::getBuyerAccountId)
                .orderByDesc(Intent::getCreatedAt)
                .orderByDesc(Intent::getId);

        Page<Intent> intentPage = page(new Page<>(page, size), wrapper);
        return new PageResult<>(intentPage.getRecords(),
                (int) intentPage.getTotal(),
                (int) intentPage.getCurrent(),
                (int) intentPage.getSize());
    }

    private boolean isHeadOfQueue(Intent intent) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId, intent.getProductId())
                .eq(Intent::getStatus, IntentStatus.QUEUED)
                .and(w -> w.lt(Intent::getCreatedAt, intent.getCreatedAt())
                        .or()
                        .eq(Intent::getCreatedAt, intent.getCreatedAt()).lt(Intent::getId, intent.getId()));
        return count(wrapper) == 0;
    }

    private boolean hasTradingIntent(Integer productId) {
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId, productId)
                .eq(Intent::getStatus, IntentStatus.TRADING);
        return count(wrapper) > 0;
    }

    private void fillBuyerUsernames(List<Intent> intents) {
        List<Integer> buyerIds = intents.stream()
                .map(Intent::getBuyerAccountId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (buyerIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<BuyerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BuyerAccount::getId, buyerIds);
        List<BuyerAccount> buyers = buyerAccountMapper.selectList(wrapper);
        Map<Integer, String> usernameMap = new HashMap<>();
        for (BuyerAccount buyer : buyers) {
            usernameMap.put(buyer.getId(), buyer.getUsername());
        }

        for (Intent intent : intents) {
            if (intent.getBuyerAccountId() != null) {
                intent.setBuyerUsername(usernameMap.get(intent.getBuyerAccountId()));
            }
        }
    }

    private IntentHistoryVO toHistoryVO(Intent intent) {
        IntentHistoryVO vo = new IntentHistoryVO();
        vo.setProductName(intent.getProductName());
        vo.setCreatedAt(intent.getCreatedAt());
        vo.setResult(intent.getStatus().getCode());
        vo.setResultAt(intent.getResultAt());
        return vo;
    }

    private Product findLatestOnlineProduct() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatus.ONLINE)
                .orderByDesc(Product::getPublishedAt)
                .orderByDesc(Product::getId)
                .last("LIMIT 1");
        return productMapper.selectOne(wrapper);
    }
}
