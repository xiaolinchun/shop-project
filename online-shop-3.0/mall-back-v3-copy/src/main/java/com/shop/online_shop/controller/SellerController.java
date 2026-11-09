package com.shop.online_shop.controller;

import com.shop.online_shop.auth.SellerAuthInterceptor;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.seller.CategoryDTO;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerModifyPwdDTO;
import com.shop.online_shop.dto.seller.SetSalePriceDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.SellerService;
import com.shop.online_shop.vo.CategoryWithCountVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 卖家端接口（文档 3.1 – 3.24）
 */
@Validated
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Resource
    private SellerService sellerService;

    @Resource
    private OrderService orderService;

    // ===================== 3.1 – 3.4 账号与发布 =====================

    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody SellerLoginDTO dto) {
        return R.ok(sellerService.login(dto));
    }

    @PostMapping("/password/modify")
    public R<Void> modifyPassword(@Valid @RequestBody SellerModifyPwdDTO dto) {
        sellerService.modifyPassword(dto);
        return R.ok();
    }

    @PostMapping("/product/image/upload")
    public R<String> uploadImage(@RequestParam(value = "file", required = false) MultipartFile file) {
        return R.ok(sellerService.uploadProductImage(file));
    }

    @PostMapping("/product/publish")
    public R<Void> publishProduct(@Valid @RequestBody PublishProductDTO dto, HttpServletRequest request) {
        sellerService.publishProduct(sellerUsername(request), dto);
        return R.ok();
    }

    // ===================== 3.5 – 3.11 商品管理 =====================

    @GetMapping("/product/list")
    public R<PageResult<Product>> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "ONLINE") String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            HttpServletRequest request) {
        return R.ok(sellerService.listProducts(sellerUsername(request), keyword, categoryId, status, page, size));
    }

    @GetMapping("/product/history")
    public R<List<Product>> listHistoryProducts(HttpServletRequest request) {
        return R.ok(sellerService.listHistoryProducts(sellerUsername(request)));
    }

    @GetMapping("/product/{id}")
    public R<Product> getProductDetail(@PathVariable Integer id, HttpServletRequest request) {
        return R.ok(sellerService.getProductDetail(sellerUsername(request), id));
    }

    @PostMapping("/product/stock/{id}")
    public R<Void> updateStock(@PathVariable Integer id,
                               @RequestParam @NotNull(message = "库存不能为空") Integer stock,
                               HttpServletRequest request) {
        sellerService.updateStock(sellerUsername(request), id, stock);
        return R.ok();
    }

    @PostMapping("/product/{id}/recategory")
    public R<Void> changeCategory(@PathVariable Integer id,
                                  @RequestParam @NotNull(message = "分类ID无效") Integer categoryId,
                                  HttpServletRequest request) {
        sellerService.changeCategory(sellerUsername(request), id, categoryId);
        return R.ok();
    }

    @PostMapping("/product/off/{id}")
    public R<Void> offlineProduct(@PathVariable Integer id, HttpServletRequest request) {
        sellerService.offlineProduct(sellerUsername(request), id);
        return R.ok();
    }

    @PostMapping("/product/on/{id}")
    public R<Void> onlineProduct(@PathVariable Integer id, HttpServletRequest request) {
        sellerService.onlineProduct(sellerUsername(request), id);
        return R.ok();
    }

    @PostMapping("/product/{id}/sale")
    public R<Void> setSalePrice(@PathVariable Integer id,
                                @Valid @RequestBody SetSalePriceDTO dto,
                                HttpServletRequest request) {
        sellerService.setSalePrice(sellerUsername(request), id, dto);
        return R.ok();
    }

    // ===================== 3.12 – 3.17 订单 =====================

    @GetMapping("/product/{productId}/orders")
    public R<PageResult<OrderHistoryVO>> getProductOrders(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
            HttpServletRequest request) {
        return R.ok(sellerService.getProductOrders(sellerUsername(request), productId, page, size));
    }

    @PostMapping("/order/{id}/confirm")
    public R<Void> confirmOrder(@PathVariable Integer id) {
        orderService.confirmOrder(id);
        return R.ok();
    }

    @PostMapping("/order/{id}/stock-done")
    public R<Void> markStockDone(@PathVariable Integer id) {
        orderService.markStockDone(id);
        return R.ok();
    }

    @PostMapping("/order/{id}/ship")
    public R<Void> shipOrder(@PathVariable Integer id) {
        orderService.shipOrder(id);
        return R.ok();
    }

    @PostMapping("/order/{id}/complete")
    public R<Void> completeOrder(@PathVariable Integer id) {
        orderService.completeOrder(id);
        return R.ok();
    }

    @PostMapping("/order/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Integer id) {
        orderService.cancelOrderBySeller(id);
        return R.ok();
    }

    // ===================== 3.18 – 3.21 分类 =====================

    @GetMapping("/category/tree")
    public R<List<CategoryWithCountVO>> getCategoryTree(HttpServletRequest request) {
        return R.ok(sellerService.getCategoryTree(sellerUsername(request)));
    }

    @PostMapping("/category")
    public R<Void> addCategory(@Valid @RequestBody CategoryDTO dto) {
        sellerService.addCategory(dto);
        return R.ok();
    }

    @PostMapping("/category/rename/{id}")
    public R<Void> renameCategory(@PathVariable Integer id,
                                  @RequestParam @NotBlank(message = "分类名称不能为空") String categoryName) {
        sellerService.renameCategory(id, categoryName);
        return R.ok();
    }

    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable Integer id) {
        sellerService.deleteCategory(id);
        return R.ok();
    }

    // ===================== 3.22 – 3.23 顾客 =====================

    @GetMapping("/customer/list")
    public R<PageResult<CustomerVO>> listCustomers(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        return R.ok(sellerService.listCustomers(page, size));
    }

    @GetMapping("/customer/{username}/history")
    public R<PageResult<OrderHistoryVO>> getCustomerHistory(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        return R.ok(sellerService.getCustomerHistory(username, page, size));
    }

    private String sellerUsername(HttpServletRequest request) {
        return (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
    }
}
