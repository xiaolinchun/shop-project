<template>
  <el-container style="max-width:550px;margin:40px auto;padding:0 20px">
    <el-page-header @back="$router.push('/seller/home')" content="修改卖家密码"></el-page-header>
    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="旧密码">
          <el-input v-model="form.oldPassword" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading">确认修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { modifySellerPwd } from '../../api/mall'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const form = ref({
  username: '',
  oldPassword: '',
  newPassword: ''
})

onMounted(() => {
  const user = localStorage.getItem('sellerUser')
  if (!user) router.replace('/seller/login')
  else form.value.username = user
})

async function submit() {
  if (!form.value.oldPassword || !form.value.newPassword) {
    return ElMessage.warning('旧密码、新密码不能为空')
  }
  if (form.value.newPassword.length < 8) {
    return ElMessage.warning('新密码至少8位')
  }
  loading.value = true
  try {
    await modifySellerPwd(form.value)
    ElMessage.success('密码修改成功，请重新登录')
    localStorage.removeItem('sellerUser')
    router.push('/seller/login')
  } finally {
    loading.value = false
  }
}
</script>
