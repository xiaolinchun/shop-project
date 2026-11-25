package com.shop.online_shop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.online_shop.common.R;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.service.BuyerAccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class BuyerAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_BUYER_ID = "buyerId";
    public static final String ATTR_BUYER_USERNAME = "buyerUsername";

    @Resource
    private BuyerAccountService buyerAccountService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return reject(response);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            return reject(response);
        }
        BuyerAccount buyer = buyerAccountService.getByToken(token);
        if (buyer == null) {
            return reject(response);
        }
        request.setAttribute(ATTR_BUYER_ID, buyer.getId());
        request.setAttribute(ATTR_BUYER_USERNAME, buyer.getUsername());
        return true;
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, "请先登录")));
        return false;
    }
}
