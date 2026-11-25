package com.shop.online_shop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.online_shop.common.R;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.service.SellerAccountService;
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
public class SellerAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_SELLER_USERNAME = "sellerUsername";

    @Resource
    private SellerAccountService sellerAccountService;

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
        Seller seller = sellerAccountService.getByToken(token);
        if (seller == null) {
            return reject(response);
        }
        request.setAttribute(ATTR_SELLER_USERNAME, seller.getUsername());
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
