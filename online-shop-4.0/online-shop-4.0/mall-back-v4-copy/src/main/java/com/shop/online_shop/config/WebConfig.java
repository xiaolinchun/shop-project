package com.shop.online_shop.config;

import com.shop.online_shop.auth.BuyerAuthInterceptor;
import com.shop.online_shop.auth.SellerAuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private BuyerAuthInterceptor buyerAuthInterceptor;

    @Resource
    private SellerAuthInterceptor sellerAuthInterceptor;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 买家端：购物车、收藏、订单、地址、售后需要登录；
        // 商品浏览 / 搜索 / 分类 / 商品评价列表 / 注册 / 登录无需认证
        registry.addInterceptor(buyerAuthInterceptor)
                .addPathPatterns("/api/buyer/cart/**",
                        "/api/buyer/favorite/**",
                        "/api/buyer/order/**",
                        "/api/buyer/address/**",
                        "/api/buyer/after-sales/**");

        // 卖家端：除登录与图片上传外均需卖家登录
        registry.addInterceptor(sellerAuthInterceptor)
                .addPathPatterns("/api/seller/**")
                .excludePathPatterns("/api/seller/login",
                        "/api/seller/product/image/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
