package com.shop.online_shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.online_shop.dto.buyer.IntentQueryVO;
import com.shop.online_shop.dto.buyer.ModifyIntentDTO;
import com.shop.online_shop.dto.buyer.SubmitIntentDTO;
import com.shop.online_shop.dto.seller.TradeMarkDTO;
import com.shop.online_shop.entity.Intent;

public interface IntentService extends IService<Intent> {
    /** FR‑15 FR‑16：自动进入交易冻结、自动递补 */
    void autoTradeAndFreeze(Integer productId);
    /** 买家提交意向，返回口令码token FR‑02 */
    String submitIntent(SubmitIntentDTO dto);
    /** 根据口令码查询意向信息 FR‑03 */
    IntentQueryVO queryByToken(String token);
    /** 修改意向姓名电话 FR‑04，不改位次 */
    void modifyByToken(ModifyIntentDTO dto);
    /** 撤销意向 FR‑05 */
    void cancelByToken(String token);
    /** 卖家标记交易结果 FR09 FR10 */
    void markTradeResult(TradeMarkDTO dto);
}
