import request from './request'

//卖家登录 FR‑06
export function sellerLogin(data) {
  return request({url:'/seller/login', method:'POST', data})
}

//上传商品图片 FR‑A10 multipart/form‑data
export function uploadProductImage(formData) {
  return request({
    url:'/seller/product/image/upload',
    method:'POST',
    data:formData,
    headers: {'Content-Type':'multipart/form-data'},
    tokenType:'seller'
  })
}

//发布商品 FR‑07/A07/A09/A10
export function publishProduct(data) {
  return request({
    url:'/seller/product/publish',
    method:'POST',
    data,
    tokenType:'seller'
  })
}

//卖家商品列表
export function getSellerProductList(params) {
  return request({
    url:'/seller/product/list',
    method:'GET',
    params,
    tokenType:'seller'
  })
}

//卖家商品详情
export function getSellerProductDetail(id) {
  return request({
    url:`/seller/product/${id}`,
    method:'GET',
    tokenType:'seller'
  })
}

//查看意向队列 FR‑08
export function getIntentList(productId) {
  return request({
    url:`/seller/intent/list/${productId}`,
    method:'GET',
    tokenType:'seller'
  })
}

//标记交易结果 FR‑09 FR‑10
export function tradeMark(data) {
  return request({
    url:'/seller/trade/mark',
    method:'POST',
    data,
    tokenType:'seller'
  })
}

//手动冻结商品
export function freezeProduct(id) {
  return request({url:`/seller/product/freeze/${id}`, method:'POST', tokenType:'seller'})
}

//手动解冻
export function unfreezeProduct(id) {
  return request({url:`/seller/product/unfreeze/${id}`, method:'POST', tokenType:'seller'})
}

//手动下架
export function offProduct(id) {
  return request({url:`/seller/product/off/${id}`, method:'POST', tokenType:'seller'})
}

//手动上架
export function onProduct(id) {
  return request({url:`/seller/product/on/${id}`, method:'POST', tokenType:'seller'})
}

//修改库存
export function updateStock(id, stock) {
  return request({
    url:`/seller/product/stock/${id}`,
    method:'POST',
    params:{stock},
    tokenType:'seller'
  })
}

//改挂分类
export function reCategory(id, categoryId) {
  return request({
    url:`/seller/product/recategory/${id}`,
    method:'POST',
    params:{categoryId},
    tokenType:'seller'
  })
}

//卖家分类树
export function getSellerCategoryTree() {
  return request({url:'/seller/category/tree', method:'GET', tokenType:'seller'})
}

//新增分类
export function addCategory(data) {
  return request({url:'/seller/category', method:'POST', data, tokenType:'seller'})
}

//重命名分类
export function renameCategory(id, categoryName) {
  return request({
    url:`/seller/category/rename/${id}`,
    method:'POST',
    params:{categoryName},
    tokenType:'seller'
  })
}

//删除分类
export function delCategory(id) {
  return request({url:`/seller/category/${id}`, method:'DELETE', tokenType:'seller'})
}

//客户列表 FR‑A11
export function getCustomerList(params) {
  return request({url:'/seller/customer/list', method:'GET', params, tokenType:'seller'})
}

//客户购买历史
export function getCustomerHistory(username, params) {
  return request({
    url:`/seller/customer/${username}/history`,
    method:'GET',
    params,
    tokenType:'seller'
  })
}

//存量匿名意向归档 RA5
export function getAnonymousIntentList(params) {
  return request({
    url:'/seller/intent/anonymous/list',
    method:'GET',
    params,
    tokenType:'seller'
  })
}

//查看历史商品 FR‑13
export function getSellerHistoryProduct() {
  return request({url:'/seller/product/history', method:'GET', tokenType:'seller'})
}

//卖家修改密码 FR‑14
export function sellerModifyPwd(data) {
  return request({url:'/seller/password/modify', method:'POST', data, tokenType:'seller'})
}
