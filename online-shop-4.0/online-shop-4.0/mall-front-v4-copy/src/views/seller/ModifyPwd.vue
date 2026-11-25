<template>
  <div style="max-width:500px;margin:60px auto;padding:24px;border:1px solid #eee;border-radius:8px">
    <h2 style="text-align:center;margin-bottom:24px">修改密码</h2>
    <el-form :model="form" label-width="100px">
      <el-form-item label="原密码">
        <el-input v-model="form.oldPwd" type="password" show-password placeholder="请输入原密码"/>
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPwd" type="password" show-password placeholder="请输入新密码"/>
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="form.confirmPwd" type="password" show-password placeholder="再次输入新密码"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">确认修改</el-button>
        <el-button @click="back">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sellerModifyPwd } from '../../api/seller'
import { useUserStore } from '../../stores/useUserStore'
const router = useRouter()
const userStore = useUserStore()

const form = ref({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const submit = async () => {
  // 简单校验
  if (!form.value.oldPwd || !form.value.newPwd || !form.value.confirmPwd) {
    ElMessage.warning('请填写全部内容')
    return
  }
  if (form.value.newPwd !== form.value.confirmPwd) {
    ElMessage.warning('两次新密码不一致！')
    return
  }
  if (form.value.newPwd.length < 8) {
    ElMessage.warning('新密码不少于 8 位')
    return
  }
  const username = userStore.sellerInfo?.username
  if (!username) {
    ElMessage.error('登录信息缺失，请重新登录')
    router.push('/seller/login')
    return
  }
  // 接口字段：username / oldPassword / newPassword（见 API 文档 4.22）
  await sellerModifyPwd({
    username,
    oldPassword: form.value.oldPwd,
    newPassword: form.value.newPwd
  })
  ElMessage.success('密码修改成功！')
  userStore.sellerLogout()
  router.push('/seller/login') // 修改成功后跳登录页，重新登录
}
const back = () => {
  router.back()
}
</script>
