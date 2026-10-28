<template>
  <div style="max-width:900px;margin:30px auto;padding:20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <div v-if="product" style="margin-top:20px;display:flex;gap:30px">
      <div>
        <el-image
          :src="product.imageUrls?.[0] || '/placeholder.png'"
          :preview-src-list="product.imageUrls || []"
          :initial-index="0"
          style="width:300px;height:300px"
          fit="cover"
        />
      </div>
      <div>
        <h2>{{product.name}}</h2>
        <p>价格：{{product.price}}</p>
        <p>库存：{{product.stock}}</p>
        <p>状态：<el-tag :type="product.status==='ONLINE'?'success':'info'">{{product.status}}</el-tag></p>
        <div style="margin-top:12px">
          <el-button type="primary" @click="handleSubmitIntent" :disabled="product.status!=='ONLINE'">提交意向排队</el-button>
        </div>
        <div style="margin-top:20px">
          <h4>商品描述</h4>
          <div v-html="product.description"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, submitIntent } from '../../api/buyer'
import { useUserStore } from '../../stores/useUserStore'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const productId = ref(Number(route.params.id))
const product = ref(null)

const loadDetail = async ()=>{

  const res = await getProductDetail(productId.value)
  product.value = res.data

}
const handleSubmitIntent = async ()=>{
  if(!userStore.buyerToken){
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return
  }
  await submitIntent()
  ElMessage.success('意向提交成功，进入排队')
}
onMounted(loadDetail)
</script>
