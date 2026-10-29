package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.vo.IntentHistoryVO;
import com.shop.online_shop.vo.PageResult;

import java.util.List;

public interface IntentService extends IService<Intent> {

    Integer submit(Integer productId, Integer buyerAccountId);

    PageResult<IntentHistoryVO> getHistory(Integer buyerAccountId, Integer page, Integer size);

    List<Intent> listQueueByProduct(Integer productId, String sellerId);

    void markTrade(Integer intentId, String operate, String sellerId);

    PageResult<IntentHistoryVO> getCustomerHistory(Integer buyerAccountId, Integer page, Integer size);

    PageResult<Intent> listAnonymousIntent(Integer page, Integer size);
}
