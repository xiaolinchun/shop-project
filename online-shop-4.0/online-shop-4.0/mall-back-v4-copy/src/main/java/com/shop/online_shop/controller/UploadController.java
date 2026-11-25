package com.shop.online_shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.common.R;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.mapper.SellerMapper;
import com.shop.online_shop.service.BuyerAccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传（复用 3.3 /api/seller/product/image/upload）。
 * 售后申请图片、评价图片均「先上传得到 URL，再随业务请求提交 URL 数组」；
 * 买家端复用同一上传接口，携带买家 token。
 */
@RestController
@RequestMapping("/api/seller/product/image")
public class UploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Resource
    private BuyerAccountService buyerAccountService;

    @Resource
    private SellerMapper sellerMapper;

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        assertLoggedIn(request);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = extension(originalFilename);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg / png 格式");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Files.copy(file.getInputStream(), dir.resolve(filename));
            return R.ok("/uploads/" + filename);
        } catch (IOException e) {
            throw new BusinessException("图片上传失败");
        }
    }

    /** 买家 / 卖家 token 均可（前端买家端复用本上传接口） */
    private void assertLoggedIn(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("请先登录");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("请先登录");
        }
        BuyerAccount buyer = buyerAccountService.getByToken(token);
        if (buyer != null) {
            return;
        }
        LambdaQueryWrapper<Seller> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seller::getToken, token);
        if (sellerMapper.selectCount(wrapper) > 0) {
            return;
        }
        throw new BusinessException("请先登录");
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
