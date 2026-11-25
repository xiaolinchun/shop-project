package com.shop.online_shop.controller;

import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.payment.PaymentNotifyDTO;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.vo.PaymentNotifyResultVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付渠道回调（4.1）
 * 需求层 4 渠道全支持；真实网关对接属技术实现，此处接收本地模拟渠道回调，
 * 并以 paymentNo 作为幂等键原子执行「可卖量校验 + 预占」。
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentNotifyController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/notify")
    public R<PaymentNotifyResultVO> notify(@RequestBody PaymentNotifyDTO dto) {
        PaymentNotifyResultVO result = paymentService.notify(dto);
        if (result.getMessage() != null) {
            return R.ok(result.getMessage(), result);
        }
        return R.ok(result);
    }
}
