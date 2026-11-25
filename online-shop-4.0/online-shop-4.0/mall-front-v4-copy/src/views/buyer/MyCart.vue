<template>
  <div style="max-width:1000px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>我的购物车</h2>

    <div v-loading="loading">
      <el-table
        ref="tableRef" :data="cartList" border style="margin-top:16px" row-key="id"
        v-if="cartList.length" @selection-change="rows => selectedRows = rows"
      >
        <el-table-column type="selection" width="50" :selectable="row => row.onSale" :reserve-selection="false" />
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
          <template #default="{ row }">¥{{ (row.unitPrice * row.quantity).toFixed(2) }}</template>
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
          <el-button @click="handleToFavorite" :disabled="!selectedRows.length">批量转收藏</el-button>
        </div>
        <div style="font-size:18px">
          已选合计：<span style="color:#e6a23c;font-weight:bold">¥{{ selectedAmount }}</span>
          <el-button type="primary" style="margin-left:16px" @click="handleCheckout" :disabled="!selectedRows.length">
            去结算（{{ selectedRows.length }}）
          </el-button>
        </div>
      </div>
    </div>

    <!-- 结算下单：选择收货地址后创建订单（2.15 + FR-C09） -->
    <el-dialog v-model="checkoutDialog" title="确认订单" width="600px">
      <div class="co-summary">
        <div v-for="row in selectedRows" :key="row.id" class="co-summary__row">
          <span>{{ row.productName }} × {{ row.quantity }}</span>
          <span>¥{{ (row.unitPrice * row.quantity).toFixed(2) }}</span>
        </div>
        <el-divider style="margin:8px 0" />
        <div class="co-summary__row" style="font-weight:bold">
          <span>应付总额（无运费）</span><span style="color:#e6a23c">¥{{ selectedAmount }}</span>
        </div>
      </div>

      <el-divider content-position="left">收货地址</el-divider>
      <AddressPicker ref="addressPickerRef" v-model="addressId" />

      <template #footer>
        <el-button @click="checkoutDialog = false">返回</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!addressId" @click="confirmCheckout">
          提交订单并去支付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * MyCart — 买家购物车页面（v4.0）
 *
 * 勾选 → 结算时选择收货地址（默认地址自动带入、可切换 / 快速新增）→ 创建订单。
 * v4.0 变化：下单对象校验改为「可卖量」，下单后订单进入「待支付」，跳转订单页引导支付。
 * 地址选择委托给 AddressPicker 业务组件（v-model 回传 addressId）。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, updateCartItemQuantity, removeCartItem, cartToFavorite, createOrder } from '../../api/buyer'
import AddressPicker from '../../components/buyer/AddressPicker.vue'

const router   = useRouter()
const cartList = ref([])
const loading  = ref(false)
const submitting = ref(false)

// ─── 勾选 ───
const tableRef     = ref(null)
const selectedRows = ref([])
const selectedAmount = computed(() =>
  selectedRows.value.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0).toFixed(2)
)

// ─── 结算弹窗 + 地址 ───
const checkoutDialog    = ref(false)
const addressId         = ref(null)
const addressPickerRef  = ref(null)

const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartList.value = res?.data || []
  } catch { cartList.value = [] }
  loading.value = false
}

const handleQuantityChange = async (id, val) => {
  if (val === 0) { await handleRemove(id); return }
  try { await updateCartItemQuantity(id, val) } catch { loadCart() }
}

const handleRemove = async (id) => {
  try {
    await removeCartItem(id)
    ElMessage.success('已移除')
    loadCart()
  } catch { /* 拦截器已提示 */ }
}

const handleToFavorite = async () => {
  if (!selectedRows.value.length) return ElMessage.warning('请选择要转收藏的条目')
  const selectedIds = selectedRows.value.map(item => item.id)
  try {
    await ElMessageBox.confirm('将选中商品转入收藏并从购物车移除？', '转收藏', { type: 'info' })
    const res = await cartToFavorite({ cartItemIds: selectedIds })
    ElMessage.success(`已收藏 ${res?.data || selectedIds.length} 件商品`)
    loadCart()
  } catch { /* 用户取消 */ }
}

// ─── 打开结算 ───
const handleCheckout = () => {
  if (!selectedRows.value.length) return ElMessage.warning('请选择要结算的商品')
  addressId.value = null
  checkoutDialog.value = true
  // 弹窗内 AddressPicker 挂载时会自动带入默认地址
  setTimeout(() => addressPickerRef.value?.refresh(), 0)
}

// ─── 提交订单 ───
const confirmCheckout = async () => {
  if (!addressId.value) return ElMessage.warning('请选择收货地址')
  submitting.value = true
  try {
    const cartItemIds = selectedRows.value.map(item => item.id)
    const res = await createOrder({ cartItemIds, addressId: addressId.value })
    if (res?.code === 200) {
      checkoutDialog.value = false
      ElMessage.success('订单创建成功，请尽快完成支付（24 小时内）')
      router.push('/buyer/orders')
    } else {
      ElMessage.error(res?.msg || '下单失败')
    }
  } catch { /* 拦截器已提示 */ }
  submitting.value = false
}

onMounted(() => loadCart())
</script>

<style scoped>
.co-summary__row { display: flex; justify-content: space-between; padding: 4px 0; }
</style>
