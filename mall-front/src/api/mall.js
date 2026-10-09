import request from './request'

//=====================买家端 /api/buyer=====================
/** FR01 获取当前在售商品 */
export function getCurrentProduct() {
  return request({ url: '/api/buyer/product/current', method: 'get' })
}

/** FR02 提交购买意向，返回口令码 */
export function submitIntent(data) {
  return request({ url: '/api/buyer/intent/submit', method: 'post', data })
}

/** FR03 根据token口令码查询意向状态 */
export function queryIntentByToken(token) {
  return request({ url: '/api/buyer/intent/query', method: 'get', params: { token } })
}

/** FR04 修改意向姓名电话 */
export function modifyIntent(data) {
  return request({ url: '/api/buyer/intent/modify', method: 'post', data })
}

/** FR05 撤销意向 */
export function cancelIntent(token) {
  return request({ url: '/api/buyer/intent/cancel', method: 'post', params: { token } })
}

//=====================卖家端 /api/seller=====================
/** FR06 卖家登录 */
export function sellerLogin(data) {
  return request({ url: '/api/seller/login', method: 'post', data })
}

/** FR07 发布商品 */
export function publishProduct(data) {
  return request({ url: '/api/seller/product/publish', method: 'post', data })
}

/** FR08 获取商品意向队列列表 */
export function getIntentList(productId) {
  return request({ url: `/api/seller/intent/list/${productId}`, method: 'get' })
}

/** FR09/10 标记交易结果 */
export function markTradeResult(data) {
  return request({ url: '/api/seller/trade/mark', method: 'post', data })
}

/** FR11冻结商品 */
export function freezeProduct(id) {
  return request({ url: `/api/seller/product/freeze/${id}`, method: 'post' })
}
/** FR11解冻商品 */
export function unfreezeProduct(id) {
  return request({ url: `/api/seller/product/unfreeze/${id}`, method: 'post' })
}
/** FR12下架商品 */
export function offProduct(id) {
  return request({ url: `/api/seller/product/off/${id}`, method: 'post' })
}
/** FR13 查询历史下架商品 */
export function getHistoryProduct() {
  return request({ url: '/api/seller/product/history', method: 'get' })
}
/** FR14 修改卖家密码 */
export function modifySellerPwd(data) {
  return request({ url: '/api/seller/password/modify', method: 'post', data })
}
