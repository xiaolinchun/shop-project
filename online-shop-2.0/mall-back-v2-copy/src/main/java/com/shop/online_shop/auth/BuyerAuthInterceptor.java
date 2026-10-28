package com.shop.online_shop.auth;

import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.service.BuyerAccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BuyerAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_BUYER_ID = "buyerId";
    public static final String ATTR_BUYER_USERNAME = "buyerUsername";

    @Resource
    private BuyerAccountService buyerAccountService;

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
        BuyerAccount buyer = buyerAccountService.getByToken(token);
        if (buyer == null) {
            throw new BusinessException("请先登录");
        }
        request.setAttribute(ATTR_BUYER_ID, buyer.getId());
        request.setAttribute(ATTR_BUYER_USERNAME, buyer.getUsername());
        return true;
    }
}
