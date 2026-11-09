<template>
  <div style="max-width:1000px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>我的收藏</h2>

    <div v-loading="loading" style="margin-top:16px">
      <div v-if="favoriteList.length" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">
        <el-card
          v-for="item in favoriteList" :key="item.productId"
          :body-style="{ padding: '16px' }"
          :class="{ 'favorite-card--off': !item.onSale }"
        >
          <div style="display:flex;gap:12px">
            <el-image
              :src="item.mainImageUrl || '/placeholder.png'"
              style="width:100px;height:100px;flex-shrink:0" fit="cover"
              @click="goDetail(item.productId)"
            />
            <div style="flex:1;min-width:0">
              <h4 style="margin:0 0 8px;cursor:pointer" @click="goDetail(item.productId)">
                {{ item.productName }}
              </h4>
              <div>
                <span v-if="item.salePrice && item.currentPrice < item.price" style="color:#e6a23c;font-weight:bold;font-size:18px">
                  ¥{{ item.currentPrice }}
                </span>
                <span v-else style="font-size:18px;font-weight:bold">¥{{ item.price }}</span>
                <span
                  v-if="item.salePrice && item.currentPrice < item.price"
                  style="text-decoration:line-through;color:#999;margin-left:8px;font-size:13px"
                >¥{{ item.price }}</span>
              </div>
              <el-tag v-if="!item.onSale" type="info" size="small" style="margin-top:6px">已下架</el-tag>
              <div style="margin-top:8px;font-size:12px;color:#999">收藏于 {{ item.createdAt }}</div>
            </div>
          </div>
          <div style="margin-top:12px;display:flex;gap:8px">
            <el-button
              size="small" type="primary"
              :disabled="!item.onSale"
              @click="handleAddCart(item)"
            >加入购物车</el-button>
            <el-button size="small" type="danger" @click="handleRemove(item.productId)">取消收藏</el-button>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无收藏商品" />
    </div>
  </div>
</template>

<script setup>
/**
 * MyFavorite — 买家收藏列表页面
 *
 * 卡片式展示，支持「加入购物车」和「取消收藏」操作。
 * 已下架商品置灰（onSale=false），不可加购但保留显示。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFavoriteList, removeFavorite, addToCart } from '../../api/buyer'
import { useUserStore } from '../../stores/useUserStore'

const router    = useRouter()
const userStore = useUserStore()

const favoriteList = ref([])
const loading      = ref(false)

// ─── 加载收藏列表 ───
const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavoriteList()
    favoriteList.value = res?.data || []
  } catch { favoriteList.value = [] }
  loading.value = false
}

// ─── 跳转商品详情 ───
const goDetail = (productId) => {
  router.push(`/buyer/detail/${productId}`)
}

// ─── 加入购物车 ───
const handleAddCart = async (item) => {
  if (!userStore.buyerToken) {
    ElMessage.warning('请先登录')
    router.push('/buyer/login')
    return
  }
  try {
    const res = await addToCart({ productId: item.productId, quantity: 1 })
    if (res?.code === 200) {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res?.msg || '加购失败')
    }
  } catch { /* 拦截器已提示 */ }
}

// ─── 取消收藏 ───
const handleRemove = async (productId) => {
  try {
    await removeFavorite(productId)
    ElMessage.success('已取消收藏')
    loadFavorites()
  } catch { /* 拦截器已提示 */ }
}

onMounted(() => loadFavorites())
</script>

<style scoped>
.favorite-card--off {
  opacity: 0.6;
}
</style>
