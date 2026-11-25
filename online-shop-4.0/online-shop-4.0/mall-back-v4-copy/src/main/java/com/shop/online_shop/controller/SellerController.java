package com.shop.online_shop.controller;

import com.shop.online_shop.auth.SellerAuthInterceptor;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.seller.AfterSalesResultDTO;
import com.shop.online_shop.dto.seller.CategoryCreateDTO;
import com.shop.online_shop.dto.seller.ExchangeShipDTO;
import com.shop.online_shop.dto.seller.LogisticsProviderDTO;
import com.shop.online_shop.dto.seller.ProductPublishDTO;
import com.shop.online_shop.dto.seller.ProductSaleDTO;
import com.shop.online_shop.dto.seller.ReturnReceivedDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerPasswordDTO;
import com.shop.online_shop.dto.seller.ShipDTO;
import com.shop.online_shop.entity.LogisticsProvider;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.SellerAccountService;
import com.shop.online_shop.service.SellerAfterSalesService;
import com.shop.online_shop.service.SellerCategoryService;
import com.shop.online_shop.service.SellerLogisticsService;
import com.shop.online_shop.service.SellerOrderService;
import com.shop.online_shop.service.SellerProductService;
import com.shop.online_shop.vo.AfterSalesOrderVO;
import com.shop.online_shop.vo.CategoryWithCountVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Resource
    private SellerAccountService sellerAccountService;

    @Resource
    private SellerProductService sellerProductService;

    @Resource
    private SellerOrderService sellerOrderService;

    @Resource
    private SellerCategoryService sellerCategoryService;

    @Resource
    private SellerLogisticsService sellerLogisticsService;

    @Resource
    private SellerAfterSalesService sellerAfterSalesService;

    // ===================== 账号 3.1 – 3.2 =====================

    /** 3.1 卖家登录，返回 token */
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody SellerLoginDTO dto) {
        return R.ok(sellerAccountService.login(dto));
    }

    /** 3.2 修改密码 */
    @PostMapping("/password/modify")
    public R<Void> modifyPassword(@Valid @RequestBody SellerPasswordDTO dto, HttpServletRequest request) {
        sellerAccountService.modifyPassword(seller(request), dto);
        return R.ok();
    }

    // ===================== 商品 3.4 – 3.11 / 3.24 =====================

    /** 3.4 发布商品 */
    @PostMapping("/product/publish")
    public R<Void> publish(@Valid @RequestBody ProductPublishDTO dto, HttpServletRequest request) {
        sellerProductService.publish(seller(request), dto);
        return R.ok();
    }

    /** 3.5 当前商品列表（在售 / 已下架） */
    @GetMapping("/product/list")
    public R<PageResult<Product>> listProducts(@RequestParam(defaultValue = "1")
                                               @Min(value = 1, message = "页码必须大于等于1") Integer page,
                                               @RequestParam(defaultValue = "10")
                                               @Min(value = 1, message = "每页条数必须大于等于1") Integer size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer categoryId,
                                               @RequestParam(required = false) String status,
                                               HttpServletRequest request) {
        return R.ok(sellerProductService.list(seller(request), keyword, categoryId, status, page, size));
    }

    /** 3.6 卖家商品详情 */
    @GetMapping("/product/{id}")
    public R<Product> productDetail(@PathVariable Integer id, HttpServletRequest request) {
        return R.ok(sellerProductService.detail(seller(request), id));
    }

    /** 3.7 历史商品（已下架） */
    @GetMapping("/product/history")
    public R<List<Product>> historyProducts(HttpServletRequest request) {
        return R.ok(sellerProductService.history(seller(request)));
    }

    /** 3.8 修改库存 */
    @PostMapping({"/product/stock/{id}", "/product/{id}/stock"})
    public R<Void> updateStock(@PathVariable Integer id,
                               @RequestParam @Min(value = 0, message = "库存不能为负")
                               @Max(value = 99999, message = "库存不能超过99999") Integer stock,
                               HttpServletRequest request) {
        sellerProductService.updateStock(seller(request), id, stock);
        return R.ok();
    }

    /** 3.9 修改分类 */
    @PostMapping("/product/{id}/recategory")
    public R<Void> reCategory(@PathVariable Integer id,
                              @RequestParam Integer categoryId,
                              HttpServletRequest request) {
        sellerProductService.reCategory(seller(request), id, categoryId);
        return R.ok();
    }

    /** 3.10 下架商品 */
    @PostMapping({"/product/off/{id}", "/product/{id}/off"})
    public R<Void> offShelf(@PathVariable Integer id, HttpServletRequest request) {
        sellerProductService.offShelf(seller(request), id);
        return R.ok();
    }

    /** 3.11 上架商品 */
    @PostMapping({"/product/on/{id}", "/product/{id}/on"})
    public R<Void> onShelf(@PathVariable Integer id, HttpServletRequest request) {
        sellerProductService.onShelf(seller(request), id);
        return R.ok();
    }

    /** 3.24 设置限时活动价 */
    @PostMapping("/product/{id}/sale")
    public R<Void> setSalePrice(@PathVariable Integer id,
                                @Valid @RequestBody ProductSaleDTO dto,
                                HttpServletRequest request) {
        sellerProductService.setSalePrice(seller(request), id, dto);
        return R.ok();
    }

    // ===================== 商品订单 3.12 – 3.17 =====================

    /** 3.12 商品订单列表（按商品维度，下单时间正序） */
    @GetMapping("/product/{productId}/orders")
    public R<PageResult<OrderHistoryVO>> productOrders(@PathVariable Integer productId,
                                                       @RequestParam(defaultValue = "1")
                                                       @Min(value = 1, message = "页码必须大于等于1") Integer page,
                                                       @RequestParam(defaultValue = "10")
                                                       @Min(value = 1, message = "每页条数必须大于等于1") Integer size,
                                                       HttpServletRequest request) {
        return R.ok(sellerOrderService.productOrders(seller(request), productId, page, size));
    }

    /** 3.13 确认订单 */
    @PostMapping("/order/{id}/confirm")
    public R<Void> confirmOrder(@PathVariable Integer id, HttpServletRequest request) {
        sellerOrderService.confirm(seller(request), id);
        return R.ok();
    }

    /** 3.14 备货完成 */
    @PostMapping("/order/{id}/stock-done")
    public R<Void> stockDone(@PathVariable Integer id, HttpServletRequest request) {
        sellerOrderService.stockDone(seller(request), id);
        return R.ok();
    }

    /** 3.15 开始发货 */
    @PostMapping("/order/{id}/ship")
    public R<Void> ship(@PathVariable Integer id,
                        @Valid @RequestBody ShipDTO dto,
                        HttpServletRequest request) {
        sellerOrderService.ship(seller(request), id, dto);
        return R.ok();
    }

    /** 3.17 取消订单 */
    @PostMapping("/order/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Integer id, HttpServletRequest request) {
        sellerOrderService.cancel(seller(request), id);
        return R.ok();
    }

    // ===================== 分类 3.18 – 3.21 =====================

    /** 3.18 分类树（含商品数） */
    @GetMapping("/category/tree")
    public R<List<CategoryWithCountVO>> categoryTree(HttpServletRequest request) {
        return R.ok(sellerCategoryService.tree(seller(request)));
    }

    /** 3.19 新增分类 */
    @PostMapping("/category")
    public R<Map<String, Integer>> addCategory(@Valid @RequestBody CategoryCreateDTO dto) {
        return R.ok(Map.of("id", sellerCategoryService.add(dto)));
    }

    /** 3.20 重命名分类 */
    @PostMapping("/category/rename/{id}")
    public R<Void> renameCategory(@PathVariable Integer id,
                                  @RequestParam @NotBlank(message = "分类名称不能为空") String categoryName) {
        sellerCategoryService.rename(id, categoryName);
        return R.ok();
    }

    /** 3.21 删除分类 */
    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable Integer id) {
        sellerCategoryService.delete(id);
        return R.ok();
    }

    // ===================== 客户 3.22 – 3.23 =====================

    /** 3.22 客户列表 */
    @GetMapping("/customer/list")
    public R<PageResult<CustomerVO>> customers(@RequestParam(defaultValue = "1")
                                               @Min(value = 1, message = "页码必须大于等于1") Integer page,
                                               @RequestParam(defaultValue = "10")
                                               @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(sellerOrderService.customers(page, size));
    }

    /** 3.23 查看客户历史订单 */
    @GetMapping("/customer/{username}/history")
    public R<PageResult<OrderHistoryVO>> customerHistory(@PathVariable String username,
                                                         @RequestParam(defaultValue = "1")
                                                         @Min(value = 1, message = "页码必须大于等于1") Integer page,
                                                         @RequestParam(defaultValue = "10")
                                                         @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(sellerOrderService.customerHistory(username, page, size));
    }

    // ===================== 物流服务商 3.25 – 3.27 =====================

    /** 3.25 物流服务商列表 */
    @GetMapping("/logistics/providers")
    public R<List<LogisticsProvider>> logisticsProviders() {
        return R.ok(sellerLogisticsService.list());
    }

    /** 3.26 新增物流服务商 */
    @PostMapping("/logistics/providers")
    public R<Map<String, Integer>> addLogisticsProvider(@Valid @RequestBody LogisticsProviderDTO dto) {
        return R.ok(Map.of("id", sellerLogisticsService.add(dto)));
    }

    /** 3.27 删除物流服务商 */
    @DeleteMapping("/logistics/providers/{id}")
    public R<Void> deleteLogisticsProvider(@PathVariable Integer id) {
        sellerLogisticsService.delete(id);
        return R.ok();
    }

    // ===================== 售后处理 3.28 – 3.32 =====================

    /** 3.28 售后单列表（按申请时间倒序） */
    @GetMapping("/after-sales/list")
    public R<PageResult<AfterSalesOrderVO>> afterSalesList(@RequestParam(defaultValue = "1")
                                                           @Min(value = 1, message = "页码必须大于等于1") Integer page,
                                                           @RequestParam(defaultValue = "10")
                                                           @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(sellerAfterSalesService.list(page, size));
    }

    /** 3.29 进入协商中 */
    @PostMapping("/after-sales/{id}/negotiating")
    public R<Void> negotiating(@PathVariable Integer id) {
        sellerAfterSalesService.negotiating(id);
        return R.ok();
    }

    /** 3.30 录入售后结果 */
    @PostMapping("/after-sales/{id}/result")
    public R<Map<String, Object>> recordResult(@PathVariable Integer id,
                                               @Valid @RequestBody AfterSalesResultDTO dto) {
        return R.ok(sellerAfterSalesService.recordResult(id, dto));
    }

    /** 3.31 确认收到退回件 */
    @PostMapping("/after-sales/{id}/return-received")
    public R<Map<String, Object>> returnReceived(@PathVariable Integer id,
                                                 @RequestBody(required = false) ReturnReceivedDTO dto) {
        return R.ok(sellerAfterSalesService.returnReceived(id, dto == null ? new ReturnReceivedDTO() : dto));
    }

    /** 3.32 换货寄出登记 */
    @PostMapping("/after-sales/{id}/exchange-ship")
    public R<Void> exchangeShip(@PathVariable Integer id,
                                @Valid @RequestBody ExchangeShipDTO dto) {
        sellerAfterSalesService.exchangeShip(id, dto);
        return R.ok();
    }

    private String seller(HttpServletRequest request) {
        return (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
    }
}
