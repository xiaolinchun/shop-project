import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/useUserStore'

const service = axios.create({
  baseURL: '/api', // vite代理会转发到后端
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(config => {
  const userStore = useUserStore()
  // 优先按显式声明的 tokenType 取对应身份 token（例如买家复用卖家图片上传接口）
  if (config.tokenType === 'buyer' && userStore.buyerToken) {
    config.headers.Authorization = `Bearer ${userStore.buyerToken}`
  } else if (config.tokenType === 'seller' && userStore.sellerToken) {
    config.headers.Authorization = `Bearer ${userStore.sellerToken}`
  } else if (config.tokenType) {
    // 显式声明了身份但对应 token 缺失：不携带任何 token（如未登录访问受限接口）
  } else if (config.url.startsWith('/buyer/') && userStore.buyerToken) {
    // 未显式声明时，回退到按 URL 前缀推断身份
    config.headers.Authorization = `Bearer ${userStore.buyerToken}`
  } else if (config.url.startsWith('/seller/') && userStore.sellerToken) {
    config.headers.Authorization = `Bearer ${userStore.sellerToken}`
  }
  // /payment/** 为渠道回调，使用渠道签名/内部令牌，前端不携带买家或卖家 token
  return config
}, err => Promise.reject(err))

// 响应拦截器
service.interceptors.response.use(res => {
  return res.data
}, err => {
  const url = err.config?.url || ''
  // ✅ 白名单：登录、注册接口，401不做登出跳转
  const authApiList = [
    '/buyer/login',
    '/seller/login',
    '/buyer/register',
    '/seller/register'
  ]
  if (authApiList.includes(url)) {
    // 登录注册的401直接抛给页面自己处理，不拦截
    return Promise.reject(err)
  }

  if (err.response?.status === 401) {
    ElMessage.error('登录失效，请重新登录')
    const userStore = useUserStore()
    // 401 清空对应身份：优先按显式 tokenType，否则回退 URL 前缀
    const tokenType = err.config?.tokenType
    if (tokenType === 'seller') {
      userStore.sellerLogout()
    } else if (tokenType === 'buyer' || err.config.url.startsWith('/buyer/')) {
      userStore.buyerLogout()
    } else {
      userStore.sellerLogout()
    }
    location.href = '/'
  } else {
    ElMessage.error(err.response?.data?.msg || '请求失败')
  }
  return Promise.reject(err)
})

export default service
