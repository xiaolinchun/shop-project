package com.shop.online_shop.config;

import com.shop.online_shop.auth.BuyerAuthInterceptor;
import com.shop.online_shop.auth.SellerAuthInterceptor;
import com.shop.online_shop.controller.SellerController;
import jakarta.annotation.Resource;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(buyerAuthInterceptor)
                .addPathPatterns("/api/buyer/intent/**");

        registry.addInterceptor(sellerAuthInterceptor)
                .addPathPatterns("/api/seller/**")
                .excludePathPatterns("/api/seller/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(SellerController.UPLOAD_DIR).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/" + SellerController.UPLOAD_DIR + "/**")
                .addResourceLocations(uploadPath);
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
