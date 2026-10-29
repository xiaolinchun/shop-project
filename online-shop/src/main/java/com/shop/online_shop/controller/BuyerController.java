package com.shop.online_shop.controller;

import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.buyer.IntentQueryVO;
import com.shop.online_shop.dto.buyer.ModifyIntentDTO;
import com.shop.online_shop.dto.buyer.SubmitIntentDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.service.ProductService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    @Resource
    private ProductService productService;
    @Resource
    private IntentService intentService;

    //FR‑01浏览当前商品
    @GetMapping("/product/current")
    public R<Product> getCurrentProduct(){
        Product p = productService.getCurrentActiveProduct();
        return R.ok(p);
    }

    //FR‑02提交购买意向，返回口令码token
    @PostMapping("/intent/submit")
    public R<String> submit(@Valid @RequestBody SubmitIntentDTO dto){
        String token = intentService.submitIntent(dto);
        return R.ok(token);
    }

    //FR‑03凭口令码查询
    @GetMapping("/intent/query")
    public R<IntentQueryVO> query(@RequestParam String token){
        IntentQueryVO vo = intentService.queryByToken(token);
        return R.ok(vo);
    }

    //FR‑04修改姓名电话
    @PostMapping("/intent/modify")
    public R<?> modify(@Valid @RequestBody ModifyIntentDTO dto){
        intentService.modifyByToken(dto);
        return R.ok();
    }

    //FR‑05撤销意向
    @PostMapping("/intent/cancel")
    public R<?> cancel(@RequestParam String token){
        intentService.cancelByToken(token);
        return R.ok();
    }

}