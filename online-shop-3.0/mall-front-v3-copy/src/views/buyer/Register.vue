<template>
  <div style="max-width:420px;margin:60px auto;padding:24px;border:1px solid #eee;border-radius:8px">
    <h2>买家注册</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username"></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password></el-input>
        <div style="font-size:12px;color:#666">密码至少8位</div>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone"></el-input>
      </el-form-item>
      <el-form-item label="默认交易地点" prop="defaultTradeLocation">
        <el-input v-model="form.defaultTradeLocation" ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleRegister">注册</el-button>
        <el-button @click="$router.push('/buyer/login')">去登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { buyerRegister } from '../../api/buyer'
import { useUserStore } from '../../stores/useUserStore'
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const form = ref({username:'',password:'',phone:'',defaultTradeLocation:''})

const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' }
  ],
  defaultTradeLocation: [
    { required: true, message: '请输入默认交易地点', trigger: 'blur' }
  ]
})

const handleRegister = async ()=>{
  try {
    await formRef.value.validate()
  } catch { return } // 表单校验不通过，el-form 已逐项标红
  try {
    const res = await buyerRegister(form.value)
    // 后端业务错误包在 HTTP 200 里，必须检查业务码
    if (res?.code !== 200 || !res.data) {
      return ElMessage.error(res?.msg || '注册失败')
    }
    userStore.setBuyerToken(res.data, form.value.username)
    ElMessage.success('注册成功，自动登录')
    router.push('/buyer/home')
  } catch {
    // HTTP 层错误，拦截器已弹提示
  }
}
</script>
