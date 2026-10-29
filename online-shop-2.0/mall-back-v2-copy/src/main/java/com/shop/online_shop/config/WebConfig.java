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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path uploadDir = Paths.get(SellerController.UPLOAD_DIR).toAbsolutePath();
        try {
            // 必须先保证目录存在再调 toUri()：Path.toUri() 仅在目录已存在时才带尾部斜杠，
            // 缺斜杠会让 Spring 把末段 uploads 当作文件名替换掉，导致 /uploads/** 全部解析不到资源
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("创建上传目录失败：" + uploadDir, e);
        }
        String uploadPath = uploadDir.toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
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
