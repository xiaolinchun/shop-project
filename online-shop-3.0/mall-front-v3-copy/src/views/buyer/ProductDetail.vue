<template>
  <div style="max-width:900px;margin:30px auto;padding:20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>

    <div v-if="product" style="margin-top:20px;display:flex;gap:30px">
      <!-- 左侧：图片 -->
      <div>
        <el-image
          :src="product.imageUrls?.[0] || '/placeholder.png'"
          :preview-src-list="product.imageUrls || []"
          :initial-index="0"
          style="width:300px;height:300px"
          fit="cover"
        />
      </div>

      <!-- 右侧：信息 -->
      <div style="flex:1">
        <h2>{{ product.name }}</h2>

        <!-- 价格区域（活动价 / 发布价） -->
        <div style="margin:12px 0">
          <span v-if="product.salePrice && product.currentPrice < product.price" style="font-size:24px;color:#e6a23c;font-weight:bold">
            ¥{{ product.currentPrice }}
          </span>
          <span v-else style="font-size:24px;font-weight:bold">
            ¥{{ product.price }}
          </span>
          <span
            v-if="product.salePrice && product.currentPrice < product.price"
            style="text-decoration:line-through;color:#999;margin-left:12px;font-size:16px"
          >
            ¥{{ product.price }}
          </span>
          <el-tag v-if="product.salePrice" type="warning" size="small" style="margin-left:8px">限时优惠</el-tag>
        </div>

        <p>库存：{{ product.stock }}</p>
        <p>
          状态：
          <el-tag :type="product.status === 'ONLINE' ? 'success' : 'info'">
            {{ product.status === 'ONLINE' ? '在售' : '已下架' }}
          </el-tag>
        </p>
        <p>分类：{{ product.categoryName }}</p>
        <p v-if="product.salePrice" style="font-size:13px;color:#999">
          活动时间：{{ product.saleStartAt }} ~ {{ product.saleEndAt }}
        </p>

        <!-- 操作按钮 -->
        <div style="margin-top:16px;display:flex;gap:10px">
          <el-button type="primary" @click="handleAddCart" :disabled="product.status !== 'ONLINE'">
            加入购物车
          </el-button>
          <el-button type="warning" @click="handleBuyNow" :disabled="product.status !== 'ONLINE'">
            立即购买
          </el-button>
          <el-button @click="handleToggleFavorite">
            {{ isFavorited ? '取消收藏' : '收藏商品' }}
          </el-button>
        </div>

        <!-- 商品描述 -->
        <div style="margin-top:20px">
          <h4>商品描述</h4>
          <div v-html="product.description || '暂无描述'" />
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />
  </div>
</template>

<script setup>
/**
 * ProductDetail — 买家商品详情页
 *
 * v3.0 变更：
 *   ① 已下架商品买家不可访问（详情打不开）
 *   ② 新增活动价展示（划线价 + 限时优惠标签）
 *   ③ 购买入口改为「加入购物车 / 立即购买」，收藏替代意向
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductDetail, addToCart, addFavorite, removeFavorite } from '../../api/buyer'
import { useUserStore } from '../../stores/useUserStore'

const route     = useRoute()
const router    = useRouter()
const userStore = useUserStore()

const productId   = ref(Number(route.params.id))
const product     = ref(null)
const loading     = ref(true)
const isFavorited = ref(false)

// ─── 加载商品详情 ───
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getProductDetail(productId.value)
    if (res?.code === 200 && res.data) {
      product.value = res.data
    } else {
      ElMessage.error(res?.msg || '商品不存在或已下架')
      router.push('/buyer/home')
    }
  } catch {
    ElMessage.error('商品不存在或已下架')
    router.push('/buyer/home')
  }
  loading.value = false
}

// ─── 加入购物车 ───
const handleAddCart = async () => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return
  }
  try {
    const res = await addToCart({ productId: productId.value, quantity: 1 })
    if (res?.code === 200) {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res?.msg || '加购失败')
    }
  } catch { /* 拦截器已提示 */ }
}

// ─── 立即购买（直接下单） ───
const handleBuyNow = async () => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return
  }
  // 跳转到购物车页面，预选当前商品（或直接创建订单）
  try {
    await ElMessageBox.confirm('确认立即购买该商品？', '立即购买', { type: 'info' })
    // 直接下单
    const { createOrder } = await import('../../api/buyer')
    const res = await createOrder({ productId: productId.value, quantity: 1 })
    if (res?.code === 200) {
      ElMessage.success('订单创建成功')
      router.push('/buyer/orders')
    } else {
      ElMessage.error(res?.msg || '下单失败')
    }
  } catch { /* 用户取消或接口报错 */ }
}

// ─── 收藏 / 取消收藏 ───
const handleToggleFavorite = async () => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return
  }
  try {
    if (isFavorited.value) {
      await removeFavorite(productId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(productId.value)
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch { /* 拦截器已提示 */ }
}

onMounted(() => loadDetail())
</script>
