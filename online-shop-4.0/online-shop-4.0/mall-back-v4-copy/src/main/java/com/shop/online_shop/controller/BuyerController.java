package com.shop.online_shop.controller;

import com.shop.online_shop.auth.BuyerAuthInterceptor;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.buyer.AddressDTO;
import com.shop.online_shop.dto.buyer.AfterSalesApplyDTO;
import com.shop.online_shop.dto.buyer.AddToCartDTO;
import com.shop.online_shop.dto.buyer.BuyerLoginDTO;
import com.shop.online_shop.dto.buyer.BuyerRegisterDTO;
import com.shop.online_shop.dto.buyer.CartToFavoriteDTO;
import com.shop.online_shop.dto.buyer.CreateOrderDTO;
import com.shop.online_shop.dto.buyer.PayRequestDTO;
import com.shop.online_shop.dto.buyer.ReviewSubmitDTO;
import com.shop.online_shop.entity.AfterSalesOrder;
import com.shop.online_shop.entity.Order;
import com.shop.online_shop.entity.PaymentRecord;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.Review;
import com.shop.online_shop.service.AddressService;
import com.shop.online_shop.service.AfterSalesService;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.service.CartService;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.FavoriteService;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.PaymentService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.ReviewService;
import com.shop.online_shop.vo.AddressVO;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.CartItemVO;
import com.shop.online_shop.vo.CategoryTreeVO;
import com.shop.online_shop.vo.ConfirmReceiptVO;
import com.shop.online_shop.vo.FavoriteVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import com.shop.online_shop.vo.ProductListItemVO;
import com.shop.online_shop.vo.ProductReviewPageVO;
import com.shop.online_shop.vo.ShippingVO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Resource
    private PaymentService paymentService;

    @Resource
    private AddressService addressService;

    @Resource
    private AfterSalesService afterSalesService;

    @Resource
    private ReviewService reviewService;

    // ===================== 商品浏览 2.1 – 2.4 =====================

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

    /** 2.30 商品评价列表（无需登录，聚合评分 + 分页列表） */
    @GetMapping("/product/{id}/reviews")
    public R<ProductReviewPageVO> productReviews(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(reviewService.pageByProduct(id, page, size));
    }

    // ===================== 账号 2.5 – 2.6 =====================

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
        return R.ok(cartService.addToCart(buyerId(request), dto.getProductId(), dto.getQuantity()));
    }

    /** 2.8 购物车列表 */
    @GetMapping("/cart")
    public R<List<CartItemVO>> cartList(HttpServletRequest request) {
        return R.ok(cartService.listCart(buyerId(request)));
    }

    /** 2.9 修改购物车数量（减到 0 等同移除） */
    @PutMapping("/cart/{id}/quantity")
    public R<Void> updateCartQuantity(@PathVariable Integer id,
                                      @RequestParam Integer quantity,
                                      HttpServletRequest request) {
        cartService.updateQuantity(buyerId(request), id, quantity);
        return R.ok();
    }

    /** 2.10 移除购物车条目 */
    @DeleteMapping("/cart/{id}")
    public R<Void> removeCartItem(@PathVariable Integer id, HttpServletRequest request) {
        cartService.removeItem(buyerId(request), id);
        return R.ok();
    }

    /** 2.11 购物车批量转收藏 */
    @PostMapping("/cart/to-favorite")
    public R<Integer> cartToFavorite(@Valid @RequestBody CartToFavoriteDTO dto, HttpServletRequest request) {
        return R.ok(cartService.cartToFavorite(buyerId(request), dto.getCartItemIds()));
    }

    // ===================== 收藏 2.12 – 2.14 =====================

    /** 2.12 收藏商品 */
    @PostMapping("/favorite/{productId}")
    public R<Void> addFavorite(@PathVariable Integer productId, HttpServletRequest request) {
        favoriteService.addFavorite(buyerId(request), productId);
        return R.ok();
    }

    /** 2.13 取消收藏 */
    @DeleteMapping("/favorite/{productId}")
    public R<Void> removeFavorite(@PathVariable Integer productId, HttpServletRequest request) {
        favoriteService.removeFavorite(buyerId(request), productId);
        return R.ok();
    }

    /** 2.14 收藏列表 */
    @GetMapping("/favorite/list")
    public R<List<FavoriteVO>> favoriteList(HttpServletRequest request) {
        return R.ok(favoriteService.listFavorites(buyerId(request)));
    }

    // ===================== 订单 2.15 – 2.17 / 2.19 – 2.20 =====================

    /** 2.15 创建订单（购物车勾选下单 / 直接下单），成功后进入待支付 */
    @PostMapping("/order")
    public R<Order> createOrder(@RequestBody CreateOrderDTO dto, HttpServletRequest request) {
        return R.ok(orderService.createOrder(buyerId(request), buyerUsername(request), dto));
    }

    /** 2.16 买家订单历史 */
    @GetMapping("/order/history")
    public R<PageResult<OrderHistoryVO>> orderHistory(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return R.ok(orderService.getHistory(buyerId(request), page, size));
    }

    /** 2.17 买家取消订单 */
    @PostMapping("/order/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Integer id, HttpServletRequest request) {
        orderService.cancelOrder(buyerId(request), id);
        return R.ok();
    }

    /** 2.19 发起支付 */
    @PostMapping("/order/{id}/pay")
    public R<PaymentRecord> pay(@PathVariable Integer id,
                                @RequestBody PayRequestDTO dto,
                                HttpServletRequest request) {
        return R.ok(paymentService.initiate(buyerId(request), id, dto.getChannel()));
    }

    /** 2.20 确认收货 */
    @PostMapping("/order/{id}/confirm-receipt")
    public R<ConfirmReceiptVO> confirmReceipt(@PathVariable Integer id, HttpServletRequest request) {
        return R.ok(orderService.confirmReceipt(buyerId(request), id));
    }

    /** 2.26 物流状态查询 */
    @GetMapping("/order/{id}/shipping")
    public R<ShippingVO> shipping(@PathVariable Integer id, HttpServletRequest request) {
        return R.ok(orderService.getShipping(buyerId(request), id));
    }

    // ===================== 地址簿 2.21 – 2.25 =====================

    /** 2.21 地址列表 */
    @GetMapping("/address/list")
    public R<List<AddressVO>> addressList(HttpServletRequest request) {
        return R.ok(addressService.listMine(buyerId(request)));
    }

    /** 2.22 新增地址 */
    @PostMapping("/address")
    public R<Map<String, Object>> addAddress(@RequestBody AddressDTO dto, HttpServletRequest request) {
        Integer id = addressService.create(buyerId(request), dto);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return R.ok(data);
    }

    /** 2.23 编辑地址 */
    @PutMapping("/address/{id}")
    public R<Void> updateAddress(@PathVariable Integer id,
                                 @RequestBody AddressDTO dto,
                                 HttpServletRequest request) {
        addressService.update(buyerId(request), id, dto);
        return R.ok();
    }

    /** 2.24 删除地址 */
    @DeleteMapping("/address/{id}")
    public R<Void> deleteAddress(@PathVariable Integer id, HttpServletRequest request) {
        addressService.delete(buyerId(request), id);
        return R.ok();
    }

    /** 2.25 设为默认地址 */
    @PostMapping("/address/{id}/default")
    public R<Void> setDefaultAddress(@PathVariable Integer id, HttpServletRequest request) {
        addressService.setDefault(buyerId(request), id);
        return R.ok();
    }

    // ===================== 售后 2.27 – 2.28 =====================

    /** 2.27 按订单明细发起售后申请 */
    @PostMapping("/order/items/{itemId}/after-sales")
    public R<Map<String, Object>> applyAfterSales(@PathVariable Integer itemId,
                                                  @RequestBody AfterSalesApplyDTO dto,
                                                  HttpServletRequest request) {
        AfterSalesOrder afterSales = afterSalesService.apply(buyerId(request), itemId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("afterSalesNo", afterSales.getAfterSalesNo());
        data.put("status", afterSales.getStatus() == null ? null : afterSales.getStatus().getCode());
        return R.ok(data);
    }

    /** 2.28 我的售后单列表 */
    @GetMapping("/after-sales/list")
    public R<PageResult<AfterSalesOrderVO>> afterSalesList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size,
            HttpServletRequest request) {
        return R.ok(afterSalesService.listMine(buyerId(request), page, size));
    }

    // ===================== 评价 2.29 =====================

    /** 2.29 按订单明细提交评价 */
    @PostMapping("/order/items/{itemId}/review")
    public R<Map<String, Object>> submitReview(@PathVariable Integer itemId,
                                               @RequestBody ReviewSubmitDTO dto,
                                               HttpServletRequest request) {
        Integer reviewId = reviewService.submit(buyerId(request), itemId, dto);
        Review review = reviewService.getById(reviewId);
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", reviewId);
        data.put("productId", review == null ? null : review.getProductId());
        return R.ok(data);
    }

    // ===================== 分类 2.18 =====================

    /** 2.18 商品分类树 */
    @GetMapping("/category/tree")
    public R<List<CategoryTreeVO>> categoryTree() {
        return R.ok(categoryService.getTree());
    }

    // ===================== 当前登录身份 =====================

    private Integer buyerId(HttpServletRequest request) {
        return (Integer) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_ID);
    }

    private String buyerUsername(HttpServletRequest request) {
        return (String) request.getAttribute(BuyerAuthInterceptor.ATTR_BUYER_USERNAME);
    }
}
