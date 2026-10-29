import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '', // vite代理，不需要填地址
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

//响应拦截器
service.interceptors.response.use(res => {
  const { code, msg, data } = res.data
  if (code === 200) {
    return data // 直接返回业务data
  } else {
    ElMessage.error(msg || '操作失败')
    return Promise.reject(new Error(msg))
  }
}, err => {
  ElMessage.error('网络请求失败')
  return Promise.reject(err)
})

export default service
