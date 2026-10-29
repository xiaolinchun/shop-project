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
  // 根据当前请求所属身份，带上对应token
  if (config.url.startsWith('/buyer/') && userStore.buyerToken) {
    config.headers.Authorization = `Bearer ${userStore.buyerToken}`
  }
  if (config.url.startsWith('/seller/') && userStore.sellerToken) {
    config.headers.Authorization = `Bearer ${userStore.sellerToken}`
  }
  return config
}, err => Promise.reject(err))

// 响应拦截器
service.interceptors.response.use(res => {
  return res.data
}, err => {
  if (err.response?.status === 401) {
    ElMessage.error('登录失效，请重新登录')
    const userStore = useUserStore()
    // 401 清空对应身份
    if (err.config.url.startsWith('/buyer/')) {
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
