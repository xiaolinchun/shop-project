package com.shop.online_shop.auth;

import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.service.SellerService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SellerAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_SELLER_USERNAME = "sellerUsername";

    @Resource
    private SellerService sellerService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("请先登录");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("请先登录");
        }
        Seller seller = sellerService.getByToken(token);
        if (seller == null) {
            throw new BusinessException("请先登录");
        }
        request.setAttribute(ATTR_SELLER_USERNAME, seller.getUsername());
        return true;
    }
}
