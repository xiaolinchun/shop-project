import request from './request'

// ──────────────── 账号 ────────────────

// 卖家登录 3.1
export function sellerLogin(data) {
  return request({ url: '/seller/login', method: 'POST', data })
}

// 修改密码 3.2
export function sellerModifyPwd(data) {
  return request({ url: '/seller/password/modify', method: 'POST', data, tokenType: 'seller' })
}

// ──────────────── 商品图片 ────────────────

// 上传商品图片 3.3
export function uploadProductImage(formData) {
  return request({
    url: '/seller/product/image/upload',
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    tokenType: 'seller'
  })
}

// ──────────────── 商品 CRUD ────────────────

// 发布商品 3.4
export function publishProduct(data) {
  return request({ url: '/seller/product/publish', method: 'POST', data, tokenType: 'seller' })
}

// 我的商品列表（分页 + 搜索 + 分类 + 状态）3.5
export function getSellerProductList(params) {
  return request({ url: '/seller/product/list', method: 'GET', params, tokenType: 'seller' })
}

// 商品详情 3.6
export function getSellerProductDetail(id) {
  return request({ url: `/seller/product/${id}`, method: 'GET', tokenType: 'seller' })
}

// 历史商品记录（全部已下架）3.7
export function getSellerHistoryProduct() {
  return request({ url: '/seller/product/history', method: 'GET', tokenType: 'seller' })
}

// 修改库存 3.8
export function updateStock(id, stock) {
  return request({ url: `/seller/product/stock/${id}`, method: 'POST', params: { stock }, tokenType: 'seller' })
}

// 改挂商品分类 3.9
export function reCategory(id, categoryId) {
  return request({ url: `/seller/product/${id}/recategory`, method: 'POST', params: { categoryId }, tokenType: 'seller' })
}

// 下架商品 3.10
export function offProduct(id) {
  return request({ url: `/seller/product/off/${id}`, method: 'POST', tokenType: 'seller' })
}

// 上架商品 3.11
export function onProduct(id) {
  return request({ url: `/seller/product/on/${id}`, method: 'POST', tokenType: 'seller' })
}

// ──────────────── 订单（卖家） ────────────────

// 商品订单记录 3.12
export function getProductOrders(productId, params) {
  return request({ url: `/seller/product/${productId}/orders`, method: 'GET', params, tokenType: 'seller' })
}

// 商家确认订单 3.13
export function confirmOrder(id) {
  return request({ url: `/seller/order/${id}/confirm`, method: 'POST', tokenType: 'seller' })
}

// 标记备货完成 3.14
export function stockDoneOrder(id) {
  return request({ url: `/seller/order/${id}/stock-done`, method: 'POST', tokenType: 'seller' })
}

// 开始发货 3.15（v4.0：必须同时登记物流服务商 + 运单号，凭预占放行）
export function shipOrder(id, data) {
  return request({ url: `/seller/order/${id}/ship`, method: 'POST', data, tokenType: 'seller' })
}

// 【已舍弃】3.16 标记交易完成：v4.0 交易完成改由买家确认收货触发（2.20）

// 卖家取消订单 3.17
export function cancelOrder(id) {
  return request({ url: `/seller/order/${id}/cancel`, method: 'POST', tokenType: 'seller' })
}

// ──────────────── 分类 ────────────────

// 分类树（含商品数）3.18
export function getSellerCategoryTree() {
  return request({ url: '/seller/category/tree', method: 'GET', tokenType: 'seller' })
}

// 新增分类 3.19
export function addCategory(data) {
  return request({ url: '/seller/category', method: 'POST', data, tokenType: 'seller' })
}

// 重命名分类 3.20
export function renameCategory(id, categoryName) {
  return request({ url: `/seller/category/rename/${id}`, method: 'POST', params: { categoryName }, tokenType: 'seller' })
}

// 删除分类 3.21
export function delCategory(id) {
  return request({ url: `/seller/category/${id}`, method: 'DELETE', tokenType: 'seller' })
}

// ──────────────── 客户 ────────────────

// 客户列表 3.22
export function getCustomerList(params) {
  return request({ url: '/seller/customer/list', method: 'GET', params, tokenType: 'seller' })
}

// 客户购买历史（订单维度）3.23
export function getCustomerHistory(username, params) {
  return request({ url: `/seller/customer/${username}/history`, method: 'GET', params, tokenType: 'seller' })
}

// ──────────────── 活动价 ────────────────

// 设置限时活动价 3.24
export function setSalePrice(id, data) {
  return request({ url: `/seller/product/${id}/sale`, method: 'POST', data, tokenType: 'seller' })
}

// ──────────────── 物流服务商 3.25 – 3.27 ────────────────

// 物流服务商列表 3.25
export function getLogisticsProviders() {
  return request({ url: '/seller/logistics/providers', method: 'GET', tokenType: 'seller' })
}

// 新增物流服务商 3.26
export function addLogisticsProvider(data) {
  return request({ url: '/seller/logistics/providers', method: 'POST', data, tokenType: 'seller' })
}

// 删除物流服务商 3.27
export function deleteLogisticsProvider(id) {
  return request({ url: `/seller/logistics/providers/${id}`, method: 'DELETE', tokenType: 'seller' })
}

// ──────────────── 售后处理 3.28 – 3.32 ────────────────

// 售后单列表 3.28
export function getSellerAfterSalesList(params) {
  return request({ url: '/seller/after-sales/list', method: 'GET', params, tokenType: 'seller' })
}

// 标记协商中 3.29
export function markAfterSalesNegotiating(id) {
  return request({ url: `/seller/after-sales/${id}/negotiating`, method: 'POST', tokenType: 'seller' })
}

// 录入售后结果 3.30
export function setAfterSalesResult(id, data) {
  return request({ url: `/seller/after-sales/${id}/result`, method: 'POST', data, tokenType: 'seller' })
}

// 确认收到退回件 3.31
export function confirmReturnReceived(id, data) {
  return request({ url: `/seller/after-sales/${id}/return-received`, method: 'POST', data, tokenType: 'seller' })
}

// 换货寄出登记 3.32
export function registerExchangeShip(id, data) {
  return request({ url: `/seller/after-sales/${id}/exchange-ship`, method: 'POST', data, tokenType: 'seller' })
}
