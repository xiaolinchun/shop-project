package com.shop.online_shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.online_shop.auth.BuyerAuthInterceptor;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.dto.buyer.SubmitIntentDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.CategoryTreeVO;
import com.shop.online_shop.vo.IntentHistoryVO;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    @Resource
    private ProductService productService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private BuyerAccountService buyerAccountService;

    @Resource
    private IntentService intentService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 3.1 浏览当前商品（FR-01，保留）
     */
    @GetMapping("/product/current")
    public R<Product> currentProduct() {
        return R.ok(productService.getCurrentOnlineProduct());
    }

    /**
     * 3.2 商品列表：分类筛选 + 排序 + 分页（FR-A05）
     */
    @GetMapping("/product/list")
    public R<PageResult<ProductListItemVO>> productList(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "TIME_DESC") String sort,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(productService.listProducts(categoryId, sort, page, size));
    }

    /**
     * 3.3 商品搜索（FR-A06）
     */
    @GetMapping("/product/search")
    public R<PageResult<ProductListItemVO>> searchProducts(
            @RequestParam
            @NotBlank(message = "关键词不能为空") String keyword,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(productService.searchProducts(keyword, page, size));
    }

    /**
     * 3.4 商品详情
     */
    @GetMapping("/product/{id}")
    public R<Product> productDetail(@PathVariable Integer id) {
        return R.ok(productService.getProductDetail(id));
    }

    /**
     * 3.5 买家注册（FR-A01）
     */
    @PostMapping("/register")
    public R<String> register(@Valid @RequestBody BuyerRegisterDTO dto) {
        return R.ok(buyerAccountService.register(dto));
    }

    /**
     * 3.6 买家登录（FR-A02）
     */
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody BuyerLoginDTO dto) {
        return R.ok(buyerAccountService.login(dto));
    }

    /**
     * 3.7 提交购买意向（FR-A03）
     * <p>文档未定义商品入参；为兼容多商品场景，支持 query 参数或 JSON 请求体中的 productId，
     * 两者都未提供时按基线逻辑取最新一件在售商品。</p>
     */
    @PostMapping("/intent/submit")
    public R<Integer> submitIntent(@RequestParam(required = false) Integer productId,
                                   HttpServletRequest request) {
        Integer effectiveProductId = productId;
        if (effectiveProductId == null) {
            effectiveProductId = readProductIdFromJsonBody(request);
        }
        Integer buyerAccountId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(intentService.submit(effectiveProductId, buyerAccountId));
    }

    /**
     * 兼容 JSON 请求体中的 productId；非 JSON 请求体或空请求体直接忽略。
     */
    private Integer readProductIdFromJsonBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)
                || !contentType.toLowerCase().contains("application/json")) {
            return null;
        }
        try {
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(body)) {
                return null;
            }
            SubmitIntentDTO dto = objectMapper.readValue(body, SubmitIntentDTO.class);
            return dto == null ? null : dto.getProductId();
        } catch (IOException e) {
            throw new BusinessException("请求参数格式错误");
        }
    }

    /**
     * 3.8 我的历史意向记录（FR-A04）
     */
    @GetMapping("/intent/history")
    public R<PageResult<IntentHistoryVO>> intentHistory(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size,
            HttpServletRequest request) {
        Integer buyerAccountId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(intentService.getHistory(buyerAccountId, page, size));
    }

    /**
     * 3.9 商品分类树
     */
    @GetMapping("/category/tree")
    public R<List<CategoryTreeVO>> categoryTree() {
        return R.ok(categoryService.getTree());
    }
}
