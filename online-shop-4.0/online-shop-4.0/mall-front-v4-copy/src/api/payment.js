import request from './request'

// ──────────────── 支付渠道回调（4.1 / 附录 A 模拟支付） ────────────────
//
// 定位：本地模拟支付页 + 渠道回调。属技术实现层，不改变正文接口契约。
// 4 渠道（银行卡 / 信用卡 / 支付宝 / 微信）在界面上全量呈现，均为本地模拟。
// 流程：买家 2.19 发起支付拿到 paymentNo → 模拟支付页选择渠道并提交
//      → 前端调用本接口模拟渠道回调 → 后端原子执行「可卖量校验 + 预占」。

// 支付渠道枚举（前端展示常量）
export const PAY_CHANNELS = [
  { value: 'BANK_CARD',   label: '银行卡',   color: '#409eff', icon: 'CreditCard' },
  { value: 'CREDIT_CARD', label: '信用卡',   color: '#e6a23c', icon: 'Postcard' },
  { value: 'ALIPAY',      label: '支付宝',   color: '#1677ff', icon: 'Wallet' },
  { value: 'WECHAT_PAY',  label: '微信支付', color: '#67c23a', icon: 'ChatDotRound' }
]

// 渠道回调 /api/payment/notify（渠道签名 / 内部令牌校验，前端不携带买家 token）
// result: SUCCESS / FAIL；paidAt 仅 SUCCESS 时必填
export function paymentNotify(data) {
  return request({ url: '/payment/notify', method: 'POST', data })
}
