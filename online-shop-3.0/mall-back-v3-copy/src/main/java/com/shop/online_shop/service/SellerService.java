package com.shop.online_shop.service;

import com.shop.online_shop.dto.seller.CategoryDTO;
import com.shop.online_shop.dto.seller.PublishProductDTO;
import com.shop.online_shop.dto.seller.SellerLoginDTO;
import com.shop.online_shop.dto.seller.SellerModifyPwdDTO;
import com.shop.online_shop.dto.seller.SetSalePriceDTO;
import com.shop.online_shop.entity.Product;
import com.shop.online_shop.entity.Seller;
import com.shop.online_shop.vo.CategoryWithCountVO;
import com.shop.online_shop.vo.CustomerVO;
import com.shop.online_shop.vo.OrderHistoryVO;
import com.shop.online_shop.vo.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 卖家端服务：账号、商品、分类、顾客（订单状态推进见 OrderService）
 */
public interface SellerService {

    /** 3.1 卖家登录，返回 token */
    String login(SellerLoginDTO dto);

    /** 3.2 修改密码 */
    void modifyPassword(SellerModifyPwdDTO dto);

    /** 根据 token 获取卖家 */
    Seller getByToken(String token);

    /** 3.3 上传商品图片，返回可访问 URL */
    String uploadProductImage(MultipartFile file);

    /** 3.4 发布商品 */
    void publishProduct(String sellerUsername, PublishProductDTO dto);

    /** 3.5 我的商品列表 */
    PageResult<Product> listProducts(String sellerUsername, String keyword, Integer categoryId,
                                     String status, Integer page, Integer size);

    /** 3.6 已下架商品记录 */
    List<Product> listHistoryProducts(String sellerUsername);

    /** 3.6 商品详情（含在途量/已发货量，仅本人商品） */
    Product getProductDetail(String sellerUsername, Integer id);

    /** 3.7 修改库存 */
    void updateStock(String sellerUsername, Integer id, Integer stock);

    /** 3.9 修改商品分类 */
    void changeCategory(String sellerUsername, Integer id, Integer categoryId);

    /** 3.10 手动下架 */
    void offlineProduct(String sellerUsername, Integer id);

    /** 3.11 立即上架 */
    void onlineProduct(String sellerUsername, Integer id);

    /** 3.24 设置限时活动价 */
    void setSalePrice(String sellerUsername, Integer id, SetSalePriceDTO dto);

    /** 3.12 商品订单记录（校验商品归属后委托订单服务） */
    PageResult<OrderHistoryVO> getProductOrders(String sellerUsername, Integer productId,
                                                Integer page, Integer size);

    /** 3.18 分类树 + 商品数 */
    List<CategoryWithCountVO> getCategoryTree(String sellerUsername);

    /** 3.19 新增分类 */
    void addCategory(CategoryDTO dto);

    /** 3.20 重命名分类 */
    void renameCategory(Integer id, String categoryName);

    /** 3.21 删除分类 */
    void deleteCategory(Integer id);

    /** 3.22 顾客列表 */
    PageResult<CustomerVO> listCustomers(Integer page, Integer size);

    /** 3.23 顾客购买历史 */
    PageResult<OrderHistoryVO> getCustomerHistory(String username, Integer page, Integer size);
}
