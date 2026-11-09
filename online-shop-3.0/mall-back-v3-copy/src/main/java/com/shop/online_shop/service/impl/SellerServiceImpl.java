package com.shop.online_shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.online_shop.common.BusinessException;
import com.shop.online_shop.dto.seller.CategoryDTO;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerModifyPwdDTO;
import com.shop.online_shop.dto.seller.SetSalePriceDTO;
import com.shop.online_shop.entity.BuyerAccount;
import com.shop.online_shop.entity.Category;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.enums.OffReason;
import com.shop.online_shop.enums.OrderStatus;
import com.shop.online_shop.enums.ProductStatus;
import com.shop.online_shop.mapper.OrderItemMapper;
import com.shop.online_shop.mapper.SellerMapper;
import com.shop.online_shop.service.BuyerAccountService;
import com.shop.online_shop.service.CategoryService;
import com.shop.online_shop.service.OrderService;
import com.shop.online_shop.service.ProductService;
import com.shop.online_shop.service.SellerService;
import com.shop.online_shop.util.TokenUtil;
import com.shop.online_shop.vo.CategoryWithCountVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SellerServiceImpl implements SellerService {

    private static final int MAX_STOCK = 99999;
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    /** 在途量口径：客户下单后、交易完成前的所有未发货数量 */
    private static final List<String> PENDING_STATUSES = List.of(
            OrderStatus.CONFIRMED.getCode(),
            OrderStatus.STOCKED.getCode(),
            OrderStatus.SHIPPING.getCode());

    /** 已发货量口径：仅 SHIPPING 状态 */
    private static final List<String> SHIPPED_STATUSES = List.of(OrderStatus.SHIPPING.getCode());

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private SellerMapper sellerMapper;

    @Resource
    private ProductService productService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private BuyerAccountService buyerAccountService;

    @Resource
    private OrderService orderService;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // ===================== 账号 =====================

    @Override
    public String login(SellerLoginDTO dto) {
        Seller seller = sellerMapper.selectById(dto.getUsername().trim());
        if (seller == null || !passwordEncoder.matches(dto.getPassword(), seller.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        seller.setToken(TokenUtil.generateToken());
        sellerMapper.updateById(seller);
        return seller.getToken();
    }

    @Override
    public void modifyPassword(SellerModifyPwdDTO dto) {
        Seller seller = sellerMapper.selectById(dto.getUsername().trim());
        if (seller == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), seller.getPasswordHash())) {
            throw new BusinessException("旧密码错误");
        }
        seller.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        sellerMapper.updateById(seller);
    }

    @Override
    public Seller getByToken(String token) {
        return sellerMapper.selectOne(new LambdaQueryWrapper<Seller>()
                .eq(Seller::getToken, token));
    }

    // ===================== 商品 =====================

    @Override
    public String uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过5MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!List.of(".jpg", ".jpeg", ".png").contains(extension)) {
            throw new BusinessException("仅支持 JPG/PNG 格式图片");
        }
        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            file.transferTo(directory.resolve(fileName).toFile());
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new BusinessException("图片上传失败");
        }
    }

    @Override
    @Transactional
    public void publishProduct(String sellerUsername, PublishProductDTO dto) {
        Category category = categoryService.getById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类ID无效");
        }
        if (category.getParentId() != null && category.getParentId() == 0) {
            throw new BusinessException("不允许挂载到一级分类");
        }

        Product product = new Product();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setImageUrls(dto.getImageUrls());
        product.setPrice(dto.getPrice());
        product.setCategoryId(dto.getCategoryId());
        product.setStock(dto.getStock());
        product.setSellerId(sellerUsername);
        product.setStatus(ProductStatus.ONLINE);
        product.setPublishedAt(LocalDateTime.now());
        productService.save(product);
    }

    @Override
    public PageResult<Product> listProducts(String sellerUsername, String keyword, Integer categoryId,
                                            String status, Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerUsername);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, parseProductStatus(status));
        }
        wrapper.orderByDesc(Product::getPublishedAt).orderByDesc(Product::getId);

        Page<Product> productPage = productService.page(new Page<>(page, size), wrapper);
        for (Product product : productPage.getRecords()) {
            fillSellerMetrics(product);
        }
        return new PageResult<>(productPage.getRecords(),
                (int) productPage.getTotal(),
                (int) productPage.getCurrent(),
                (int) productPage.getSize());
    }

    @Override
    public List<Product> listHistoryProducts(String sellerUsername) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, sellerUsername)
                .eq(Product::getStatus, ProductStatus.OFFLINE)
                .orderByDesc(Product::getClosedAt)
                .orderByDesc(Product::getId);
        List<Product> products = productService.list(wrapper);
        for (Product product : products) {
            fillSellerMetrics(product);
        }
        return products;
    }

    @Override
    public Product getProductDetail(String sellerUsername, Integer id) {
        Product product = requireOwnedProduct(sellerUsername, id);
        fillSellerMetrics(product);
        return product;
    }

    @Override
    @Transactional
    public void updateStock(String sellerUsername, Integer id, Integer stock) {
        Product product = requireOwnedProduct(sellerUsername, id);
        if (stock == null || stock < 0) {
            throw new BusinessException("库存不能为负");
        }
        if (stock > MAX_STOCK) {
            throw new BusinessException("库存不能超过" + MAX_STOCK);
        }
        if (stock == 0 && orderItemMapper.countActiveOrdersByProductId(id) > 0) {
            throw new BusinessException("存在进行中的订单，请先处理完订单");
        }

        product.setStock(stock);
        if (stock == 0) {
            // 库存归零：自动下架，原因 STOCK_OUT
            product.setStatus(ProductStatus.OFFLINE);
            product.setOffReason(OffReason.STOCK_OUT);
            product.setClosedAt(LocalDateTime.now());
        }
        productService.updateById(product);
    }

    @Override
    @Transactional
    public void changeCategory(String sellerUsername, Integer id, Integer categoryId) {
        requireOwnedProduct(sellerUsername, id);
        if (categoryId == null || categoryId < 1) {
            throw new BusinessException("分类ID无效");
        }
        Category category = categoryService.getById(categoryId);
        if (category == null) {
            throw new BusinessException("分类须存在");
        }
        if (category.getParentId() != null && category.getParentId() == 0) {
            throw new BusinessException("不允许挂载到一级分类");
        }
        productService.lambdaUpdate()
                .eq(Product::getId, id)
                .set(Product::getCategoryId, categoryId)
                .update();
    }

    @Override
    @Transactional
    public void offlineProduct(String sellerUsername, Integer id) {
        Product product = requireOwnedProduct(sellerUsername, id);
        if (product.getStatus() == ProductStatus.OFFLINE) {
            return;
        }
        if (orderItemMapper.countActiveOrdersByProductId(id) > 0) {
            throw new BusinessException("存在未完成订单，无法下架");
        }
        product.setStatus(ProductStatus.OFFLINE);
        product.setOffReason(OffReason.MANUAL);
        product.setClosedAt(LocalDateTime.now());
        productService.updateById(product);
    }

    @Override
    @Transactional
    public void onlineProduct(String sellerUsername, Integer id) {
        Product product = requireOwnedProduct(sellerUsername, id);
        if (stockOf(product) == 0) {
            throw new BusinessException("库存为0，无法上架");
        }
        if (product.getStatus() == ProductStatus.ONLINE) {
            return;
        }
        productService.lambdaUpdate()
                .eq(Product::getId, id)
                .set(Product::getStatus, ProductStatus.ONLINE)
                .set(Product::getOffReason, null)
                .set(Product::getClosedAt, null)
                .update();
    }

    @Override
    @Transactional
    public void setSalePrice(String sellerUsername, Integer id, SetSalePriceDTO dto) {
        Product product = requireOwnedProduct(sellerUsername, id);
        if (dto.getSalePrice() == null || dto.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("活动价必须大于0");
        }
        if (dto.getSaleStartAt() == null || dto.getSaleEndAt() == null) {
            throw new BusinessException("请选择活动起止时间");
        }
        if (!dto.getSaleEndAt().isAfter(dto.getSaleStartAt())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        // AB-24：单商品同一时间仅允许一个活动，未结束前不能再设
        if (product.getSalePrice() != null && product.getSaleEndAt() != null
                && LocalDateTime.now().isBefore(product.getSaleEndAt())) {
            throw new BusinessException("该商品已有进行中的活动");
        }

        product.setSalePrice(dto.getSalePrice());
        product.setSaleStartAt(dto.getSaleStartAt());
        product.setSaleEndAt(dto.getSaleEndAt());
        productService.updateById(product);
    }

    @Override
    public PageResult<OrderHistoryVO> getProductOrders(String sellerUsername, Integer productId,
                                                       Integer page, Integer size) {
        requireOwnedProduct(sellerUsername, productId);
        return orderService.getProductOrders(productId, page, size);
    }

    // ===================== 分类 =====================

    @Override
    public List<CategoryWithCountVO> getCategoryTree(String sellerUsername) {
        Map<Integer, Integer> counts = productCounts(sellerUsername);
        List<Category> categories = categoryService.lambdaQuery()
                .orderByAsc(Category::getId)
                .list();

        Map<Integer, CategoryWithCountVO> nodeMap = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryWithCountVO node = new CategoryWithCountVO();
            node.setId(category.getId());
            node.setParentId(category.getParentId());
            node.setCategoryName(category.getCategoryName());
            node.setProductCount(counts.getOrDefault(category.getId(), 0));
            nodeMap.put(category.getId(), node);
        }

        List<CategoryWithCountVO> roots = new ArrayList<>();
        for (CategoryWithCountVO node : nodeMap.values()) {
            CategoryWithCountVO parent = node.getParentId() == null ? null : nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public void addCategory(CategoryDTO dto) {
        String name = dto.getCategoryName() == null ? "" : dto.getCategoryName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (dto.getParentId() != 0 && categoryService.getById(dto.getParentId()) == null) {
            throw new BusinessException("父分类不存在");
        }
        if (categoryService.lambdaQuery()
                .eq(Category::getParentId, dto.getParentId())
                .eq(Category::getCategoryName, name)
                .count() > 0) {
            throw new BusinessException("分类名称已存在");
        }

        Category category = new Category();
        category.setParentId(dto.getParentId());
        category.setCategoryName(name);
        categoryService.save(category);
    }

    @Override
    @Transactional
    public void renameCategory(Integer id, String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            throw new BusinessException("分类名称不能为空");
        }
        Category category = categoryService.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        category.setCategoryName(categoryName.trim());
        categoryService.updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (productService.lambdaQuery().eq(Product::getCategoryId, id).count() > 0) {
            throw new BusinessException("分类下存在商品，无法删除");
        }
        if (categoryService.lambdaQuery().eq(Category::getParentId, id).count() > 0) {
            throw new BusinessException("分类下存在子分类，无法删除");
        }
        categoryService.removeById(id);
    }

    // ===================== 顾客 =====================

    @Override
    public PageResult<CustomerVO> listCustomers(Integer page, Integer size) {
        LambdaQueryWrapper<BuyerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BuyerAccount::getCreatedAt).orderByDesc(BuyerAccount::getId);
        Page<BuyerAccount> accountPage = buyerAccountService.page(new Page<>(page, size), wrapper);

        List<CustomerVO> list = new ArrayList<>();
        for (BuyerAccount account : accountPage.getRecords()) {
            CustomerVO vo = new CustomerVO();
            vo.setUsername(account.getUsername());
            vo.setPhone(account.getPhone());
            vo.setDefaultTradeLocation(account.getDefaultTradeLocation());
            vo.setCreatedAt(account.getCreatedAt());
            list.add(vo);
        }
        return new PageResult<>(list,
                (int) accountPage.getTotal(),
                (int) accountPage.getCurrent(),
                (int) accountPage.getSize());
    }

    @Override
    public PageResult<OrderHistoryVO> getCustomerHistory(String username, Integer page, Integer size) {
        BuyerAccount buyer = buyerAccountService.getByUsername(username);
        if (buyer == null) {
            throw new BusinessException("顾客不存在");
        }
        return orderService.getCustomerOrders(buyer.getId(), page, size);
    }

    // ===================== 内部方法 =====================

    private Product requireOwnedProduct(String sellerUsername, Integer id) {
        Product product = productService.getById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!sellerUsername.equals(product.getSellerId())) {
            throw new BusinessException("无权访问");
        }
        return product;
    }

    /**
     * 卖家商品卡片补充：在途量 / 已发货量 / 库存预警（口径与 3.12 一致）
     */
    private void fillSellerMetrics(Product product) {
        productService.fillTransientFields(product);
        int pending = quantitySum(product.getId(), PENDING_STATUSES);
        int shipped = quantitySum(product.getId(), SHIPPED_STATUSES);
        product.setPendingQuantity(pending);
        product.setShippedQuantity(shipped);
        // AB-05：在途量 ≥ 库存时标红提示
        product.setPendingAlert(pending > 0 && pending >= stockOf(product));
    }

    private int quantitySum(Integer productId, List<String> statuses) {
        Integer sum = orderItemMapper.sumQuantityByProductAndStatuses(productId, statuses);
        return sum == null ? 0 : sum;
    }

    private Map<Integer, Integer> productCounts(String sellerUsername) {
        List<Product> products = productService.lambdaQuery()
                .eq(Product::getSellerId, sellerUsername)
                .list();
        Map<Integer, Integer> counts = new HashMap<>();
        for (Product product : products) {
            counts.merge(product.getCategoryId(), 1, Integer::sum);
        }
        return counts;
    }

    private int stockOf(Product product) {
        return product.getStock() == null ? 0 : product.getStock();
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("商品状态不正确");
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return originalFilename.substring(index).toLowerCase(Locale.ROOT);
    }
}
