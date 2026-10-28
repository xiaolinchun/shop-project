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
    // 买家登录接口（后端 data 直接返回 token 字符串）
    async buyerLogin(payload) {
      const res = await api.post('/buyer/login', payload)
      if (res.code === 200) {
        this.buyerToken = res.data
        this.buyerInfo = { username: payload.username }
        localStorage.setItem('buyerToken', this.buyerToken)
        localStorage.setItem('buyerInfo', JSON.stringify(this.buyerInfo))
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
    }
  }
})
