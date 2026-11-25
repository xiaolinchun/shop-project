<template>
  <div style="max-width:960px;margin:30px auto;padding:20px">
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

        <!-- 聚合评分（FR-C22） -->
        <div style="margin:6px 0;display:flex;align-items:center;gap:10px">
          <template v-if="product.reviewCount">
            <el-rate :model-value="Number(product.averageRating) || 0" disabled allow-half />
            <span style="color:#e6a23c;font-weight:bold">{{ Number(product.averageRating).toFixed(1) }}</span>
            <a href="#reviews" style="color:#409eff;font-size:13px">{{ product.reviewCount }} 条评价</a>
          </template>
          <span v-else class="muted">暂无评价</span>
        </div>

        <!-- 价格区域（活动价 / 发布价） -->
        <div style="margin:12px 0">
          <span v-if="product.salePrice && product.currentPrice < product.price" style="font-size:24px;color:#e6a23c;font-weight:bold">
            ¥{{ product.currentPrice }}
          </span>
          <span v-else style="font-size:24px;font-weight:bold">¥{{ product.price }}</span>
          <span
            v-if="product.salePrice && product.currentPrice < product.price"
            style="text-decoration:line-through;color:#999;margin-left:12px;font-size:16px"
          >¥{{ product.price }}</span>
          <el-tag v-if="product.salePrice" type="warning" size="small" style="margin-left:8px">限时优惠</el-tag>
        </div>

        <!-- 库存 / 可卖量（账面 − 预占，RA-C2） -->
        <p>
          库存：{{ product.stock }}　可卖量：
          <span :style="(product.availableQuantity ?? product.stock) <= 0 ? 'color:#f56c6c;font-weight:bold' : ''">
            {{ product.availableQuantity ?? product.stock }}
          </span>
          <el-tag v-if="(product.availableQuantity ?? product.stock) <= 0" type="danger" size="small">缺货风险</el-tag>
        </p>
        <p v-if="(product.reservedQuantity ?? 0) > 0" class="muted">（其中 {{ product.reservedQuantity }} 件已被已支付订单预占）</p>
        <p>
          状态：
          <el-tag :type="product.status === 'ONLINE' ? 'success' : 'info'">
            {{ product.status === 'ONLINE' ? '在售' : '已下架' }}
          </el-tag>
        </p>
        <p>分类：{{ product.categoryName }}</p>
        <p v-if="product.salePrice" class="muted">活动时间：{{ product.saleStartAt }} ~ {{ product.saleEndAt }}</p>

        <!-- 操作按钮 -->
        <div style="margin-top:16px;display:flex;gap:10px">
          <el-button type="primary" @click="handleAddCart" :disabled="product.status !== 'ONLINE'">加入购物车</el-button>
          <el-button type="warning" @click="openBuyNow" :disabled="product.status !== 'ONLINE'">立即购买</el-button>
          <el-button @click="handleToggleFavorite">{{ isFavorited ? '取消收藏' : '收藏商品' }}</el-button>
        </div>

        <!-- 商品描述 -->
        <div style="margin-top:20px">
          <h4>商品描述</h4>
          <div v-html="product.description || '暂无描述'" />
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="商品不存在或已下架" />

    <!-- 商品评价列表（2.30） -->
    <div v-if="product" id="reviews" style="margin-top:30px">
      <h3>商品评价</h3>
      <ProductReviewList :product-id="productId" />
    </div>

    <!-- 立即购买：选择数量 + 收货地址（2.15 + FR-C09） -->
    <el-dialog v-model="buyDialog" title="立即购买" width="560px">
      <el-form label-width="90px">
        <el-form-item label="购买数量">
          <el-input-number v-model="buyQuantity" :min="1" :max="999" />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">收货地址</el-divider>
      <AddressPicker ref="addressPickerRef" v-model="addressId" />
      <template #footer>
        <el-button @click="buyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!addressId" @click="confirmBuyNow">提交订单并去支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * ProductDetail — 买家商品详情页（v4.0）
 *
 * v4.0 变更：
 *   ① 展示可卖量 / 预占量（RA-C2）与聚合评分（FR-C22）
 *   ② 页面底部内嵌 ProductReviewList 业务组件（2.30 评价列表）
 *   ③ 立即购买改为弹窗选地址 + 数量，下单后可卖量校验 + 进入待支付
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, addToCart, addFavorite, removeFavorite, createOrder } from '../../api/buyer'
import { useUserStore } from '../../stores/useUserStore'
import ProductReviewList from '../../components/buyer/ProductReviewList.vue'
import AddressPicker from '../../components/buyer/AddressPicker.vue'

const route     = useRoute()
const router    = useRouter()
const userStore = useUserStore()

const productId   = ref(Number(route.params.id))
const product     = ref(null)
const loading     = ref(true)
const isFavorited = ref(false)

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

const requireLogin = () => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录买家账号')
    router.push('/buyer/login')
    return false
  }
  return true
}

const handleAddCart = async () => {
  if (!requireLogin()) return
  try {
    const res = await addToCart({ productId: productId.value, quantity: 1 })
    if (res?.code === 200) ElMessage.success('已加入购物车')
    else ElMessage.error(res?.msg || '加购失败')
  } catch { /* 拦截器已提示 */ }
}

// ─── 立即购买 ───
const buyDialog       = ref(false)
const buyQuantity     = ref(1)
const addressId       = ref(null)
const addressPickerRef = ref(null)
const submitting      = ref(false)

const openBuyNow = () => {
  if (!requireLogin()) return
  buyQuantity.value = 1
  addressId.value = null
  buyDialog.value = true
  setTimeout(() => addressPickerRef.value?.refresh(), 0)
}

const confirmBuyNow = async () => {
  if (!addressId.value) return ElMessage.warning('请选择收货地址')
  submitting.value = true
  try {
    const res = await createOrder({ productId: productId.value, quantity: buyQuantity.value, addressId: addressId.value })
    if (res?.code === 200) {
      buyDialog.value = false
      ElMessage.success('订单创建成功，请尽快完成支付')
      router.push('/buyer/orders')
    } else {
      ElMessage.error(res?.msg || '下单失败')
    }
  } catch { /* 拦截器已提示 */ }
  submitting.value = false
}

const handleToggleFavorite = async () => {
  if (!requireLogin()) return
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

<style scoped>
.muted { color: #909399; font-size: 13px; }
</style>
