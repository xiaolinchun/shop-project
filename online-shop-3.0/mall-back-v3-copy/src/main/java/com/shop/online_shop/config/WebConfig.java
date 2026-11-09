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

import java.nio.file.Path;
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
        // 买家端：购物车、收藏、订单需要登录；商品浏览、搜索、分类、注册、登录无需认证
        registry.addInterceptor(buyerAuthInterceptor)
                .addPathPatterns("/api/buyer/cart/**",
                        "/api/buyer/favorite/**",
                        "/api/buyer/order/**");

        // 卖家端：除登录外全部需要卖家 token
        registry.addInterceptor(sellerAuthInterceptor)
                .addPathPatterns("/api/seller/**")
                .excludePathPatterns("/api/seller/login");
    }

    /**
     * 卖家上传的商品图片通过 /uploads/** 直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + directory + "/");
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
