import request from './request'

// 浏览当前商品
export function getCurrentProduct() {
  return request({url:'/buyer/product/current', method:'GET'})
}

//商品列表：分类筛选+排序+分页 FR‑A05
export function getBuyerProductList(params) {
  return request({url:'/buyer/product/list', method:'GET', params})
}

//商品搜索 FR‑A06
export function searchProduct(params) {
  return request({url:'/buyer/product/search', method:'GET', params})
}

//商品详情
export function getProductDetail(id) {
  return request({url:`/buyer/product/${id}`, method:'GET'})
}

//买家注册 FR‑A01
export function buyerRegister(data) {
  return request({url:'/buyer/register', method:'POST', data})
}

//买家登录 FR‑A02
export function buyerLogin(data) {
  return request({url:'/buyer/login', method:'POST', data})
}

//提交购买意向 FR‑A03 需要买家登录
export function submitIntent() {
  return request({
    url:'/buyer/intent/submit',
    method:'POST',
    tokenType:'buyer'
  })
}

//我的历史意向 FR‑A04
export function getBuyerIntentHistory(params) {
  return request({
    url:'/buyer/intent/history',
    method:'GET',
    params,
    tokenType:'buyer'
  })
}

//买家分类树
export function getBuyerCategoryTree() {
  return request({url:'/buyer/category/tree', method:'GET'})
}
