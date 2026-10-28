package com.shop.online_shop.controller;

import com.shop.online_shop.auth.SellerAuthInterceptor;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.common.R;
import com.shop.online_shop.dto.seller.AddCategoryDTO;
import com.shop.online_shop.dto.seller.ModifyPasswordDTO;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.TradeMarkDTO;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.Intent;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.IntentService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.SellerService;
import com.shop.online_shop.vo.CategoryWithCountVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.IntentHistoryVO;
import com.shop.online_shop.vo.PageResult;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    public static final String UPLOAD_DIR = "uploads";

    @Resource
    private SellerService sellerService;

    @Resource
    private ProductService productService;

    @Resource
    private IntentService intentService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private BuyerAccountService buyerAccountService;

    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody SellerLoginDTO dto) {
        return R.ok(sellerService.login(dto));
    }

    @PostMapping("/password/modify")
    public R<Void> modifyPassword(@Valid @RequestBody ModifyPasswordDTO dto) {
        sellerService.modifyPassword(dto);
        return R.ok();
    }

    @PostMapping("/product/image/upload")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 JPG/PNG 格式图片");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Path dir = Paths.get(UPLOAD_DIR).toAbsolutePath();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            throw new BusinessException("图片保存失败");
        }
        return R.ok("/" + UPLOAD_DIR + "/" + filename);
    }

    @PostMapping("/product/publish")
    public R<Void> publish(@Valid @RequestBody PublishProductDTO dto,
                           HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.publish(dto, sellerId);
        return R.ok();
    }

    @GetMapping("/product/list")
    public R<PageResult<Product>> productList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        return R.ok(productService.listSellerProducts(sellerId, status, page, size));
    }

    @GetMapping("/product/{id}")
    public R<Product> productDetail(@PathVariable Integer id,
                                    HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        return R.ok(productService.getSellerProduct(id, sellerId));
    }

    @GetMapping("/product/history")
    public R<List<Product>> productHistory(HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        return R.ok(productService.listSellerHistory(sellerId));
    }

    @PostMapping("/product/stock/{id}")
    public R<Void> updateStock(@PathVariable Integer id,
                               @RequestParam
                               @Min(value = 0, message = "库存不能为负") Integer stock,
                               HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.updateStock(id, stock, sellerId);
        return R.ok();
    }

    @PostMapping("/product/recategory/{id}")
    public R<Void> changeCategory(@PathVariable Integer id,
                                  @RequestParam Integer categoryId,
                                  HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.changeCategory(id, categoryId, sellerId);
        return R.ok();
    }

    @PostMapping("/product/freeze/{id}")
    public R<Void> freeze(@PathVariable Integer id, HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.freeze(id, sellerId);
        return R.ok();
    }

    @PostMapping("/product/unfreeze/{id}")
    public R<Void> unfreeze(@PathVariable Integer id, HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.unfreeze(id, sellerId);
        return R.ok();
    }

    @PostMapping("/product/off/{id}")
    public R<Void> offShelf(@PathVariable Integer id, HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.offShelf(id, sellerId);
        return R.ok();
    }

    @PostMapping("/product/on/{id}")
    public R<Void> onShelf(@PathVariable Integer id, HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        productService.onShelf(id, sellerId);
        return R.ok();
    }

    @GetMapping("/intent/list/{productId}")
    public R<List<Intent>> intentQueue(@PathVariable Integer productId,
                                       HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        return R.ok(intentService.listQueueByProduct(productId, sellerId));
    }

    @PostMapping("/trade/mark")
    public R<Void> markTrade(@Valid @RequestBody TradeMarkDTO dto,
                             HttpServletRequest request) {
        String sellerId = (String) request.getAttribute(SellerAuthInterceptor.ATTR_SELLER_USERNAME);
        intentService.markTrade(dto.getIntentId(), dto.getOperate(), sellerId);
        return R.ok();
    }

    @GetMapping("/intent/anonymous/list")
    public R<PageResult<Intent>> anonymousIntentList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(intentService.listAnonymousIntent(page, size));
    }

    @GetMapping("/category/tree")
    public R<List<CategoryWithCountVO>> categoryTree() {
        return R.ok(categoryService.getTreeWithProductCount());
    }

    @PostMapping("/category")
    public R<Void> addCategory(@Valid @RequestBody AddCategoryDTO dto) {
        categoryService.addCategory(dto);
        return R.ok();
    }

    @PostMapping("/category/rename/{id}")
    public R<Void> renameCategory(@PathVariable Integer id,
                                  @RequestParam
                                  @NotBlank(message = "分类名称不能为空") String categoryName) {
        categoryService.renameCategory(id, categoryName);
        return R.ok();
    }

    @DeleteMapping("/category/{id}")
    public R<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return R.ok();
    }

    @GetMapping("/customer/list")
    public R<PageResult<CustomerVO>> customerList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        return R.ok(buyerAccountService.listCustomers(page, size));
    }

    @GetMapping("/customer/{username}/history")
    public R<PageResult<IntentHistoryVO>> customerHistory(
            @PathVariable String username,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于等于1") Integer page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于等于1") Integer size) {
        BuyerAccount buyer = buyerAccountService.getByUsername(username);
        if (buyer == null) {
            throw new BusinessException("顾客不存在");
        }
        return R.ok(intentService.getCustomerHistory(buyer.getId(), page, size));
    }
}
