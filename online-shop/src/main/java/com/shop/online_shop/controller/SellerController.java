package com.shop.online_shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.seller.ModifyPwdDTO;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.TradeMarkDTO;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.enums.ProductStatusEnum;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.SellerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController


@RequestMapping("/api/seller")
public class SellerController {
    @Resource
    private SellerService sellerService;
    @Resource
    private ProductService productService;
    @Resource
    private IntentService intentService;

    //FR‑06卖家登录
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody SellerLoginDTO dto){
        boolean ok = sellerService.login(dto);
        if(!ok){
            throw new BusinessException("用户名或密码错误");
        }
        return R.ok("登录成功");
    }

    //FR‑07发布商品
    @PostMapping("/product/publish")
    public R<?> publish(@Valid @RequestBody PublishProductDTO dto){
        productService.publish(dto);
        return R.ok();
    }

    //FR‑08查看意向队列
    @GetMapping("/intent/list/{productId}")
    public R<List<Intent>> intentList(@PathVariable Integer productId){
        LambdaQueryWrapper<Intent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Intent::getProductId,productId).orderByAsc(Intent::getCreatedAt);
        List<Intent> list = intentService.list(wrapper);
        //注意：返回给前端不要返回token口令码！SRS卖家不可见口令码
        list.forEach(i->i.setToken(null));
        return R.ok(list);
    }

    //FR09 FR10标记交易结果
    @PostMapping("/trade/mark")
    public R<?> mark(@Valid @RequestBody TradeMarkDTO dto){
        intentService.markTradeResult(dto);
        return R.ok();
    }

    //FR‑11手动冻结
    @PostMapping("/product/freeze/{id}")
    public R<?> freeze(@PathVariable Integer id){
        productService.manualFreeze(id);
        return R.ok();
    }

    //FR‑11手动解冻
    @PostMapping("/product/unfreeze/{id}")
    public R<?> unfreeze(@PathVariable Integer id){
        productService.manualUnFreeze(id);
        return R.ok();
    }

    //FR‑12手动下架
    @PostMapping("/product/off/{id}")
    public R<?> offShelve(@PathVariable Integer id){
        productService.manualOffShelf(id);
        return R.ok();
    }

    //FR‑13查看历史商品
    @GetMapping("/product/history")
    public R<List<Product>> history(){
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, ProductStatusEnum.OFFLINE).orderByDesc(Product::getClosedAt);
        List<Product> list = productService.list(wrapper);
        return R.ok(list);
    }

    //FR‑14修改密码
    @PostMapping("/password/modify")
    public R<?> changePwd(@Valid @RequestBody ModifyPwdDTO dto){
        sellerService.changePassword(dto);
        return R.ok();
    }
}