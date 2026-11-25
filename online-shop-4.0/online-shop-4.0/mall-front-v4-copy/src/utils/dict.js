/**
 * dict.js — v4.0 业务字典与口径集中管理
 *
 * 把订单状态机（7 态 + 已取消）、支付渠道、售后单、物流跟踪、地址快照等
 * 「枚举 → 文案 / 标签颜色」的映射统一收敛在这里，供业务组件与页面复用，
 * 避免此前 statusLabel / statusType 在各视图重复定义的问题。
 */

// ──────────────── 订单状态机（7 推进态 + 已取消终态） ────────────────
export const ORDER_STATUS = {
  CREATED:          '客户下单',
  PENDING_PAYMENT:  '待支付',
  PAID:             '已支付',
  CONFIRMED:        '商家确认',
  STOCKED:          '备货完成',
  SHIPPING:         '开始发货',
  COMPLETED:        '交易完成',
  CANCELLED:        '已取消'
}

const ORDER_STATUS_TAG = {
  CREATED: 'info',
  PENDING_PAYMENT: 'warning',
  PAID: 'primary',
  CONFIRMED: 'warning',
  STOCKED: 'info',
  SHIPPING: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}

export const orderStatusLabel = (s) => ORDER_STATUS[s] || s || '-'
export const orderStatusType  = (s) => ORDER_STATUS_TAG[s] || ''

// ──────────────── 支付渠道（4 渠道全支持） ────────────────
export const PAY_CHANNEL = {
  BANK_CARD:   '银行卡',
  CREDIT_CARD: '信用卡',
  ALIPAY:      '支付宝',
  WECHAT_PAY:  '微信支付'
}
export const payChannelLabel = (c) => PAY_CHANNEL[c] || c || '-'

// ──────────────── 售后单（平行单据） ────────────────
// 状态：申请中 → 协商中 → 已闭环
export const AFTER_SALES_STATUS = {
  APPLYING:    '申请中',
  NEGOTIATING: '协商中',
  CLOSED:      '已闭环'
}
const AFTER_SALES_STATUS_TAG = {
  APPLYING: 'warning',
  NEGOTIATING: 'primary',
  CLOSED: 'success'
}
export const afterSalesStatusLabel = (s) => AFTER_SALES_STATUS[s] || s || '-'
export const afterSalesStatusType  = (s) => AFTER_SALES_STATUS_TAG[s] || ''

// 结果：退货退款 / 仅退款 / 换货 / 驳回
export const AFTER_SALES_RESULT = {
  REFUND_AND_RETURN: '退货退款',
  REFUND_ONLY:       '仅退款',
  EXCHANGE:          '换货',
  REJECTED:          '驳回'
}
const AFTER_SALES_RESULT_TAG = {
  REFUND_AND_RETURN: 'danger',
  REFUND_ONLY: 'warning',
  EXCHANGE: 'primary',
  REJECTED: 'info'
}
export const afterSalesResultLabel = (r) => AFTER_SALES_RESULT[r] || r || '-'
export const afterSalesResultType  = (r) => AFTER_SALES_RESULT_TAG[r] || ''

// ──────────────── 物流跟踪状态 ────────────────
export const TRACKING_STATUS = {
  PICKED_UP:        '揽收',
  IN_TRANSIT:       '运输中',
  OUT_FOR_DELIVERY: '派送中',
  DELIVERED:        '已签收'
}
export const trackingStatusLabel = (s) => TRACKING_STATUS[s] || s || '-'

// ──────────────── 地址快照格式化 ────────────────
// addressSnapshot: { name, phone, province, city, district, detail }
export function formatAddress(snap) {
  if (!snap) return '-'
  const region = [snap.province, snap.city, snap.district].filter(Boolean).join('')
  return `${snap.name || ''} ${snap.phone || ''}　${region}${snap.detail || ''}`.trim()
}

// ──────────────── 订单可执行动作判定（买家侧兜底，后端未回传标记时使用） ────────────────
// 买家可取消窗口：待支付 / 已支付 / 商家确认 / 备货完成（FR-C06）
export const BUYER_CANCELLABLE = ['PENDING_PAYMENT', 'CREATED', 'PAID', 'CONFIRMED', 'STOCKED']
// 买家可发起售后窗口：开始发货 / 交易完成（FR-C12）
export const AFTER_SALES_WINDOW = ['SHIPPING', 'COMPLETED']
