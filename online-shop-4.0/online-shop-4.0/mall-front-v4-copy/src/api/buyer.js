import request from './request'

// ──────────────── 商品浏览 ────────────────

// 浏览当前最新在售商品 2.1
export function getCurrentProduct() {
  return request({ url: '/buyer/product/current', method: 'GET' })
}

// 商品列表（搜索 + 分类 + 排序 + 分页）2.2
export function getBuyerProductList(params) {
  return request({ url: '/buyer/product/list', method: 'GET', params })
}

// 商品搜索（强制 keyword 的兼容入口）2.3
export function searchProduct(params) {
  return request({ url: '/buyer/product/search', method: 'GET', params })
}

// 商品详情 2.4
export function getProductDetail(id) {
  return request({ url: `/buyer/product/${id}`, method: 'GET' })
}

// ──────────────── 账号 ────────────────

// 买家注册 2.5
export function buyerRegister(data) {
  return request({ url: '/buyer/register', method: 'POST', data })
}

// 买家登录 2.6
export function buyerLogin(data) {
  return request({ url: '/buyer/login', method: 'POST', data })
}

// ──────────────── 购物车 2.7 – 2.11 ────────────────

// 加入购物车
export function addToCart(data) {
  return request({ url: '/buyer/cart', method: 'POST', data, tokenType: 'buyer' })
}

// 购物车列表
export function getCartList() {
  return request({ url: '/buyer/cart', method: 'GET', tokenType: 'buyer' })
}

// 修改购物车数量
export function updateCartItemQuantity(id, quantity) {
  return request({ url: `/buyer/cart/${id}/quantity`, method: 'PUT', params: { quantity }, tokenType: 'buyer' })
}

// 移除购物车条目
export function removeCartItem(id) {
  return request({ url: `/buyer/cart/${id}`, method: 'DELETE', tokenType: 'buyer' })
}

// 购物车批量转收藏
export function cartToFavorite(data) {
  return request({ url: '/buyer/cart/to-favorite', method: 'POST', data, tokenType: 'buyer' })
}

// ──────────────── 收藏 2.12 – 2.14 ────────────────

// 收藏商品
export function addFavorite(productId) {
  return request({ url: `/buyer/favorite/${productId}`, method: 'POST', tokenType: 'buyer' })
}

// 取消收藏
export function removeFavorite(productId) {
  return request({ url: `/buyer/favorite/${productId}`, method: 'DELETE', tokenType: 'buyer' })
}

// 收藏列表
export function getFavoriteList() {
  return request({ url: '/buyer/favorite/list', method: 'GET', tokenType: 'buyer' })
}

// ──────────────── 订单 2.15 – 2.17 ────────────────

// 创建订单（购物车勾选 / 直接下单）
export function createOrder(data) {
  return request({ url: '/buyer/order', method: 'POST', data, tokenType: 'buyer' })
}

// 买家订单历史
export function getBuyerOrderHistory(params) {
  return request({ url: '/buyer/order/history', method: 'GET', params, tokenType: 'buyer' })
}

// 买家取消订单
export function cancelBuyerOrder(id) {
  return request({ url: `/buyer/order/${id}/cancel`, method: 'POST', tokenType: 'buyer' })
}

// ──────────────── 支付 / 确认收货 2.19 – 2.20 ────────────────

// 发起支付（返回 paymentNo，status=PROCESSING）2.19
export function payOrder(id, channel) {
  return request({ url: `/buyer/order/${id}/pay`, method: 'POST', data: { channel }, tokenType: 'buyer' })
}

// 确认收货（SHIPPING → COMPLETED，预占转正式扣减）2.20
export function confirmReceipt(id) {
  return request({ url: `/buyer/order/${id}/confirm-receipt`, method: 'POST', tokenType: 'buyer' })
}

// ──────────────── 地址簿 2.21 – 2.25 ────────────────

// 地址列表 2.21
export function getAddressList() {
  return request({ url: '/buyer/address/list', method: 'GET', tokenType: 'buyer' })
}

// 新增地址 2.22
export function addAddress(data) {
  return request({ url: '/buyer/address', method: 'POST', data, tokenType: 'buyer' })
}

// 编辑地址 2.23
export function updateAddress(id, data) {
  return request({ url: `/buyer/address/${id}`, method: 'PUT', data, tokenType: 'buyer' })
}

// 删除地址 2.24
export function deleteAddress(id) {
  return request({ url: `/buyer/address/${id}`, method: 'DELETE', tokenType: 'buyer' })
}

// 设为默认地址 2.25
export function setDefaultAddress(id) {
  return request({ url: `/buyer/address/${id}/default`, method: 'POST', tokenType: 'buyer' })
}

// ──────────────── 物流查询 2.26 ────────────────

// 物流状态查询 2.26
export function getOrderShipping(orderId) {
  return request({ url: `/buyer/order/${orderId}/shipping`, method: 'GET', tokenType: 'buyer' })
}

// ──────────────── 售后 2.27 – 2.28 ────────────────

// 按订单明细发起售后申请 2.27
export function createAfterSales(itemId, data) {
  return request({ url: `/buyer/order/items/${itemId}/after-sales`, method: 'POST', data, tokenType: 'buyer' })
}

// 我的售后单列表 2.28
export function getMyAfterSalesList(params) {
  return request({ url: '/buyer/after-sales/list', method: 'GET', params, tokenType: 'buyer' })
}

// ──────────────── 评价 2.29 – 2.30 ────────────────

// 按订单明细提交评价 2.29
export function createReview(itemId, data) {
  return request({ url: `/buyer/order/items/${itemId}/review`, method: 'POST', data, tokenType: 'buyer' })
}

// 商品评价列表（无需登录）2.30
export function getProductReviews(productId, params) {
  return request({ url: `/buyer/product/${productId}/reviews`, method: 'GET', params, tokenType: 'buyer' })
}

// ──────────────── 分类 ────────────────

// 买家分类树 2.18
export function getBuyerCategoryTree() {
  return request({ url: '/buyer/category/tree', method: 'GET' })
}
