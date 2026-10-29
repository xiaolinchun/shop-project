<<<<<<< HEAD
<template>
  <el-container style="max-width:420px;margin:80px auto;padding:20px">
    <el-card header="卖家登录">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login" :loading="loading" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="margin-top:10px;text-align:center">
        <el-button link @click="$router.push('/buyer')">前往买家页面</el-button>
      </div>
    </el-card>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sellerLogin } from '../../api/mall'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

async function login() {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('用户名密码不能为空')
  }
  loading.value = true
  try {
    // ========== 硬编码模拟登录（代替后端接口） ==========
    // API文档示例账号：seller01 / 12345678
    // if (form.value.username === 'seller01' && form.value.password === '12345678') {
    //   // 模拟接口等待，模拟网络延迟效果
    //   await new Promise(resolve => setTimeout(resolve, 300))
    //   localStorage.setItem('sellerUser', form.value.username)
    //   ElMessage.success('登录成功')
    //   router.push('/seller/home')
    // } else {
    //   // 账号密码错误，和后端返回msg保持一致
    //   throw new Error('用户名或密码错误')
    // }

    // ========== 真实接口请求 =============================
    await sellerLogin(form.value)
    localStorage.setItem('sellerUser', form.value.username)
    ElMessage.success('登录成功')
    router.push('/seller/home')
  } finally {
    loading.value = false
  }
}
</script>
=======
<template>
  <el-container style="max-width:420px;margin:80px auto;padding:20px">
    <el-card header="卖家登录">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login" :loading="loading" style="width:100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="margin-top:10px;text-align:center">
        <el-button link @click="$router.push('/buyer')">前往买家页面</el-button>
      </div>
    </el-card>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { sellerLogin } from '../../api/mall'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

async function login() {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('用户名密码不能为空')
  }
  loading.value = true
  try {
    // ========== 硬编码模拟登录（代替后端接口） ==========
    // API文档示例账号：seller01 / 12345678
    // if (form.value.username === 'seller01' && form.value.password === '12345678') {
    //   // 模拟接口等待，模拟网络延迟效果
    //   await new Promise(resolve => setTimeout(resolve, 300))
    //   localStorage.setItem('sellerUser', form.value.username)
    //   ElMessage.success('登录成功')
    //   router.push('/seller/home')
    // } else {
    //   // 账号密码错误，和后端返回msg保持一致
    //   throw new Error('用户名或密码错误')
    // }

    // ========== 真实接口请求 =============================
    await sellerLogin(form.value)
    localStorage.setItem('sellerUser', form.value.username)
    ElMessage.success('登录成功')
    router.push('/seller/home')
  } finally {
    loading.value = false
  }
}
</script>
>>>>>>> 541088904427b67d3edbb35c4de84031bb23e55e
