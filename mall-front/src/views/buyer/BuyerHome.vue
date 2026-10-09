<template>
  <el-container style="max-width: 1000px; margin:40px auto; padding:0 20px">
    <el-card header="商品详情" style="width:500px">
      <div v-if="product">
        <el-image :src="product.imageUrl" fit="contain" style="height:300px; width:300px"></el-image>
        <h2>{{ product.name }}</h2>
        <p>状态：{{ PRODUCT_STATUS[product.status] }}</p>
        <p>价格：¥{{ product.price }}</p>
        <p>描述：{{ product.description || '无' }}</p>
        <p>上架时间：{{ product.publishedAt }}</p>
      </div>
      <div v-else>
        <el-empty description="暂无在售商品"></el-empty>
      </div>
    </el-card>

    <el-card header="提交购买意向" style="margin-top:20px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.buyerName"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交意向</el-button>
          <el-button @click="$router.push('/buyer/query')">查询我的意向</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="info" @click="$router.push('/')" style="width:120px">返回主页面</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentProduct, submitIntent } from '../../api/mall'
import { PRODUCT_STATUS } from '../../constants'
import { ElMessage } from 'element-plus'

const router = useRouter()
const product = ref(null)
const submitting = ref(false)
const form = ref({
  buyerName: '',
  phone: ''
})

async function loadProduct() {
  try {
    product.value = await getCurrentProduct()
  } catch (e) {}
  
  // 硬编码模拟商品，和接口文档样例保持一致
  // product.value = {
  //   id: 1,
  //   name: "iPhone 15",
  //   description: "全新未拆封",
  //   imageUrl: "https://cdn.example.com/1.jpg",
  //   price: 5999.00,
  //   status: "ONLINE",
  //   publishedAt: "2026-10-01 10:00:00",
  //   closedAt: null,
  //   sellerId: "seller01"
  // }
}

onMounted(() => {
  loadProduct()
})

async function handleSubmit() {
  if (!form.value.buyerName || !form.value.phone) {
    return ElMessage.warning('姓名和手机号不能为空')
  }
  submitting.value = true
  try {
    const token = await submitIntent(form.value)
    localStorage.setItem('buyerToken', token)
    ElMessage.success('提交成功！口令码已保存本地')
    await navigator.clipboard.writeText(token)
    ElMessage.info(`口令码已复制：${token}`)
    router.push('/buyer/query')
  } finally {
    submitting.value = false
  }
}
</script>
