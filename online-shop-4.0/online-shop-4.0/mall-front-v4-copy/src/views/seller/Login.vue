<template>
  <div style="max-width:400px;margin:80px auto;padding:24px;border:1px solid #eee;border-radius:8px">
    <h2>卖家登录</h2>
    <el-form :model="form" label-width="80px">
      <el-form-item label="账号">
        <el-input v-model="form.username"></el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin" :loading="loading">登录</el-button>
      </el-form-item>
    </el-form>
    <div style="margin-top:15px;text-align:center">
      <el-button link @click="$router.push('/')">返回选择页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../../stores/useUserStore'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await userStore.sellerLogin(form.value)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '登录失败，请检查账号密码')
      return
    }
    ElMessage.success('卖家登录成功')
    router.push('/seller/product-list')
  } catch (err) {
    ElMessage.error('登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>
