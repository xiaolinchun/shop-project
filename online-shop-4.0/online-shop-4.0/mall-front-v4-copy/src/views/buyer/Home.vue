<template>
  <div style="max-width:1200px;margin:30px auto;padding:0 20px">
    <h2>商品列表</h2>

    <!-- 买家商品列表业务组件 -->
    <BuyerProductTable
      ref="productTableRef"
      @detail="handleDetail"
      @add-cart="handleAddCart"
    />
  </div>
</template>

<script setup>
/**
 * BuyerHome — 买家商品浏览首页
 *
 * 核心列表委托给 BuyerProductTable 业务组件，
 * 页面只负责「查看详情」与「加入购物车」的交互。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/useUserStore'
import { addToCart } from '../../api/buyer'
import BuyerProductTable from '../../components/buyer/BuyerProductTable.vue'

const router = useRouter()
const userStore = useUserStore()
const productTableRef = ref(null)

// 查看详情
const handleDetail = (id) => {
  router.push(`/buyer/detail/${id}`)
}

// 加入购物车
const handleAddCart = async (product) => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return
  }
  try {
    const res = await addToCart({ productId: product.id, quantity: 1 })
    if (res?.code === 200) {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res?.msg || '加购失败')
    }
  } catch { /* 拦截器已提示 */ }
}
</script>
