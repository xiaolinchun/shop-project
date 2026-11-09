package com.shop.online_shop.controller;

import com.shop.online_shop.auth.BuyerAuthInterceptor;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.buyer.AddToCartDTO;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.dto.buyer.CartToFavoriteDTO;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.service.CartService;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.FavoriteService;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.vo.CartItemVO;
import com.shop.online_shop.vo.CategoryTreeVO;
import com.shop.online_shop.vo.FavoriteVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private CartService cartService;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private OrderService orderService;

    // ===================== 商品浏览 =====================

    /** 2.1 浏览当前最新在售商品 */
    @GetMapping("/product/current")
    public R<Product> currentProduct() {
        return R.ok(productService.getCurrentOnlineProduct());
    }

    /** 2.2 商品列表（搜索 + 分类 + 排序 + 分页） */
    @GetMapping("/product/list")
    public R<PageResult<ProductListItemVO>> productList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "TIME_DESC") String sort,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(productService.listProducts(keyword, categoryId, sort, page, size));
    }

    /** 2.3 商品搜索（强制 keyword 的兼容入口） */
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

    /** 2.4 商品详情（已下架商品买家不可访问） */
    @GetMapping("/product/{id}")
    public R<Product> productDetail(@PathVariable Integer id) {
        return R.ok(productService.getBuyerProductDetail(id));
    }

    // ===================== 账号 =====================

    /** 2.5 买家注册 */
    @PostMapping("/register")
    public R<String> register(@Valid @RequestBody BuyerRegisterDTO dto) {
        return R.ok(buyerAccountService.register(dto));
    }

    /** 2.6 买家登录 */
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody BuyerLoginDTO dto) {
        return R.ok(buyerAccountService.login(dto));
    }

    // ===================== 购物车 2.7 – 2.11 =====================

    /** 2.7 加入购物车 */
    @PostMapping("/cart")
    public R<Integer> addToCart(@Valid @RequestBody AddToCartDTO dto, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(cartService.addToCart(buyerId, dto.getProductId(), dto.getQuantity()));
    }

    /** 2.8 购物车列表 */
    @GetMapping("/cart")
    public R<List<CartItemVO>> cartList(HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(cartService.listCart(buyerId));
    }

    /** 2.9 修改购物车数量（减到 0 等同移除） */
    @PutMapping("/cart/{id}/quantity")
    public R<Void> updateCartQuantity(@PathVariable Integer id,
                                      @RequestParam Integer quantity,
                                      HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        cartService.updateQuantity(buyerId, id, quantity);
        return R.ok();
    }

    /** 2.10 移除购物车条目 */
    @DeleteMapping("/cart/{id}")
    public R<Void> removeCartItem(@PathVariable Integer id, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        cartService.removeItem(buyerId, id);
        return R.ok();
    }

    /** 2.11 购物车批量转收藏 */
    @PostMapping("/cart/to-favorite")
    public R<Integer> cartToFavorite(@Valid @RequestBody CartToFavoriteDTO dto, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(cartService.cartToFavorite(buyerId, dto.getCartItemIds()));
    }

    // ===================== 收藏 2.12 – 2.14 =====================

    /** 2.12 收藏商品 */
    @PostMapping("/favorite/{productId}")
    public R<Void> addFavorite(@PathVariable Integer productId, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        favoriteService.addFavorite(buyerId, productId);
        return R.ok();
    }

    /** 2.13 取消收藏 */
    @DeleteMapping("/favorite/{productId}")
    public R<Void> removeFavorite(@PathVariable Integer productId, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        favoriteService.removeFavorite(buyerId, productId);
        return R.ok();
    }

    /** 2.14 收藏列表 */
    @GetMapping("/favorite/list")
    public R<List<FavoriteVO>> favoriteList(HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(favoriteService.listFavorites(buyerId));
    }

    // ===================== 订单 2.15 – 2.17 =====================

    /** 2.15 创建订单（购物车勾选下单 / 直接下单） */
    @PostMapping("/order")
    public R<Order> createOrder(@RequestBody CreateOrderDTO dto, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        String buyerUsername = (String) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_USERNAME);
        return R.ok(orderService.createOrder(buyerId, buyerUsername, dto));
    }

    /** 2.16 买家订单历史 */
    @GetMapping("/order/history")
    public R<PageResult<OrderHistoryVO>> orderHistory(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        return R.ok(orderService.getHistory(buyerId, page, size));
    }

    /** 2.17 买家取消订单 */
    @PostMapping("/order/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Integer id, HttpServletRequest request) {
        Integer buyerId = (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
        orderService.cancelOrder(buyerId, id);
        return R.ok();
    }

    // ===================== 分类 =====================

    /** 2.18 商品分类树 */
    @GetMapping("/category/tree")
    public R<List<CategoryTreeVO>> categoryTree() {
        return R.ok(categoryService.getTree());
    }
}
