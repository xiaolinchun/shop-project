<template>
  <div style="max-width:1000px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>我的购物车</h2>

    <div v-loading="loading">
      <el-table :data="cartList" border style="margin-top:16px" v-if="cartList.length">
        <el-table-column type="selection" width="50" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <el-image
                :src="row.mainImageUrl || '/placeholder.png'"
                style="width:60px;height:60px;flex-shrink:0" fit="cover"
              />
              <div>
                <div :style="{ opacity: row.onSale ? 1 : 0.5 }">{{ row.productName }}</div>
                <el-tag v-if="!row.onSale" type="info" size="small">已下架</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.unitPrice < (row.originalPrice || row.unitPrice) ? '#e6a23c' : '' }">
              ¥{{ row.unitPrice }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity" :min="0" :max="999" size="small"
              @change="(val) => handleQuantityChange(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100">
          <template #default="{ row }">
            ¥{{ (row.unitPrice * row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="购物车是空的" />

      <!-- 底部操作栏 -->
      <div v-if="cartList.length" style="margin-top:16px;display:flex;justify-content:space-between;align-items:center">
        <div>
          <el-button @click="handleToFavorite" :disabled="!hasSelection">批量转收藏</el-button>
        </div>
        <div style="font-size:18px">
          合计：<span style="color:#e6a23c;font-weight:bold">¥{{ totalAmount }}</span>
          <el-button type="primary" style="margin-left:16px" @click="handleCheckout" :disabled="!hasSelection">
            结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * MyCart — 买家购物车页面
 *
 * 购物车条目列表 + 数量修改 + 删除 + 批量转收藏 + 结算下单。
 * 已下架商品置灰不可勾选（onSale=false）。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, updateCartItemQuantity, removeCartItem, cartToFavorite, createOrder } from '../../api/buyer'

const router   = useRouter()
const cartList = ref([])
const loading  = ref(false)

// ─── 计算总价 ───
const totalAmount = computed(() => {
  return cartList.value
    .filter(item => item.onSale)
    .reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
    .toFixed(2)
})

const hasSelection = computed(() => cartList.value.some(item => item.onSale))

// ─── 加载购物车 ───
const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartList.value = res?.data || []
  } catch { cartList.value = [] }
  loading.value = false
}

// ─── 修改数量 ───
const handleQuantityChange = async (id, val) => {
  if (val === 0) {
    // 减到 0 等同移除
    await handleRemove(id)
    return
  }
  try {
    await updateCartItemQuantity(id, val)
  } catch { loadCart() }
}

// ─── 删除条目 ───
const handleRemove = async (id) => {
  try {
    await removeCartItem(id)
    ElMessage.success('已移除')
    loadCart()
  } catch { /* 拦截器已提示 */ }
}

// ─── 批量转收藏 ───
const handleToFavorite = async () => {
  const selectedIds = cartList.value.filter(item => item.onSale).map(item => item.id)
  if (!selectedIds.length) return ElMessage.warning('请选择要转收藏的条目')
  try {
    await ElMessageBox.confirm('将选中商品转入收藏并从购物车移除？', '转收藏', { type: 'info' })
    const res = await cartToFavorite({ cartItemIds: selectedIds })
    ElMessage.success(`已收藏 ${res?.data || selectedIds.length} 件商品`)
    loadCart()
  } catch { /* 用户取消 */ }
}

// ─── 结算下单 ───
const handleCheckout = async () => {
  const selectedIds = cartList.value.filter(item => item.onSale).map(item => item.id)
  if (!selectedIds.length) return ElMessage.warning('请选择要结算的商品')
  try {
    await ElMessageBox.confirm(`确认下单 ${selectedIds.length} 件商品？`, '结算确认', { type: 'info' })
    const res = await createOrder({ cartItemIds: selectedIds })
    if (res?.code === 200) {
      ElMessage.success('订单创建成功')
      router.push('/buyer/orders')
    } else {
      ElMessage.error(res?.msg || '下单失败')
    }
  } catch { /* 用户取消或接口报错 */ }
}

onMounted(() => loadCart())
</script>
