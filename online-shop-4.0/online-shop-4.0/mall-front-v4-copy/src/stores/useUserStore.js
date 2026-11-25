import { defineStore } from 'pinia'
import api from '../api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    buyerToken: localStorage.getItem('buyerToken') || '',
    sellerToken: localStorage.getItem('sellerToken') || '',
    buyerInfo: JSON.parse(localStorage.getItem('buyerInfo') || 'null'),
    sellerInfo: JSON.parse(localStorage.getItem('sellerInfo') || 'null')
  }),
  actions: {
    // 注册成功后直接写入登录态（后端 data 返回 token 字符串）
    setBuyerToken(token, username) {
      this.buyerToken = token
      this.buyerInfo = { username }
      localStorage.setItem('buyerToken', token)
      localStorage.setItem('buyerInfo', JSON.stringify(this.buyerInfo))
      this.clearSellerState() // 买家身份互斥：清掉卖家状态
    },
    // 买家登录接口（后端 data 直接返回 token 字符串）
    async buyerLogin(payload) {
      const res = await api.post('/buyer/login', payload)
      if (res.code === 200) {
        this.buyerToken = res.data
        this.buyerInfo = { username: payload.username }
        localStorage.setItem('buyerToken', this.buyerToken)
        localStorage.setItem('buyerInfo', JSON.stringify(this.buyerInfo))
        this.clearSellerState() // 买家身份互斥：清掉卖家状态
      }
      return res
    },
    // 卖家登录接口（后端 data 直接返回 token 字符串；sellerInfo 保存用户名供修改密码等接口使用）
    async sellerLogin(payload) {
      const res = await api.post('/seller/login', payload)
      if (res.code === 200) {
        this.sellerToken = res.data
        this.sellerInfo = { username: payload.username }
        localStorage.setItem('sellerToken', this.sellerToken)
        localStorage.setItem('sellerInfo', JSON.stringify(this.sellerInfo))
        this.clearBuyerState() // 卖家身份互斥：清掉买家状态
      }
      return res
    },
    // 买家退出
    buyerLogout() {
      this.buyerToken = ''
      this.buyerInfo = null
      localStorage.removeItem('buyerToken')
      localStorage.removeItem('buyerInfo')
    },
    // 卖家退出
    sellerLogout() {
      this.sellerToken = ''
      this.sellerInfo = null
      localStorage.removeItem('sellerToken')
      localStorage.removeItem('sellerInfo')
    },
    // 内部辅助：清掉买家状态（卖家登录时互斥调用）
    clearBuyerState() {
      this.buyerToken = ''
      this.buyerInfo = null
      localStorage.removeItem('buyerToken')
      localStorage.removeItem('buyerInfo')
    },
    // 内部辅助：清掉卖家状态（买家登录时互斥调用）
    clearSellerState() {
      this.sellerToken = ''
      this.sellerInfo = null
      localStorage.removeItem('sellerToken')
      localStorage.removeItem('sellerInfo')
    }
  }
})
