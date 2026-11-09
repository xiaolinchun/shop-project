<template>
  <div style="max-width:1400px;margin:30px auto;padding:20px">
    <!-- 顶部操作栏 -->
    <div style="margin-bottom:16px">
      <el-button type="primary" @click="$router.push('/seller/publish')">发布新商品</el-button>
    </div>

    <!-- 卖家商品列表业务组件 -->
    <SellerProductTable
      ref="productTableRef"
      @detail="openDetail"
      @order-manage="openProductOrderManage"
      @edit-stock="openStockDialog"
      @edit-category="openCategoryDialog"
      @set-sale="openSaleDialog"
      @offline="handleOffline"
      @online="handleOnline"
    />

    <!-- 修改库存弹窗 -->
    <el-dialog v-model="stockDialog.show" title="修改库存" width="400px">
      <el-form label-width="80px">
        <el-form-item label="库存数量">
          <el-input-number v-model="stockDialog.stock" :min="0" :max="99999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialog.show = false">取消</el-button>
        <el-button type="primary" @click="confirmStock">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 改挂分类弹窗 -->
    <el-dialog v-model="categoryDialog.show" title="改挂商品分类" width="400px">
      <el-form label-width="100px">
        <el-form-item label="目标分类">
          <el-select v-model="categoryDialog.categoryId" placeholder="选择二级分类">
            <el-option v-for="c in flatCategory" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.show = false">取消</el-button>
        <el-button type="primary" @click="confirmCategory">确认</el-button>
      </template>
    </el-dialog>

    <!-- 设置活动价弹窗 -->
    <el-dialog v-model="saleDialog.show" title="设置限时活动价" width="500px">
      <el-form :model="saleDialog" label-width="100px">
        <el-form-item label="活动价">
          <el-input-number v-model="saleDialog.salePrice" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="saleDialog.saleStartAt" type="datetime"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="saleDialog.saleEndAt" type="datetime"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saleDialog.show = false">取消</el-button>
        <el-button type="primary" @click="confirmSale">确认设置</el-button>
      </template>
    </el-dialog>

    <!-- 商品详情弹窗 -->
    <el-dialog v-model="detailDialog.show" title="商品详情" width="650px">
      <div v-if="detailData" style="line-height:2.2">
        <p>商品ID：{{ detailData.id }}</p>
        <p>商品名称：{{ detailData.name }}</p>
        <p>图片展示：</p>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-image
            v-for="(img, idx) in (detailData.imageUrls || [])"
            :key="idx" :src="img"
            :preview-src-list="detailData.imageUrls"
            :initial-index="idx" style="width:120px" fit="contain"
          />
        </div>
        <p>发布价：¥{{ detailData.price }}</p>
        <p v-if="detailData.salePrice">
          活动价：<span style="color:#e6a23c;font-weight:bold">¥{{ detailData.salePrice }}</span>
          （{{ detailData.saleStartAt }} ~ {{ detailData.saleEndAt }}）
        </p>
        <p>适用价：¥{{ detailData.currentPrice ?? detailData.price }}</p>
        <p>库存：{{ detailData.stock }}</p>
        <p>
          状态：
          <el-tag :type="detailData.status === 'ONLINE' ? 'success' : 'info'">
            {{ detailData.status === 'ONLINE' ? '在售' : '已下架' }}
          </el-tag>
        </p>
        <p>分类：{{ detailData.categoryName }}</p>
        <p>发布时间：{{ detailData.publishedAt || '-' }}</p>
        <p>下架时间：{{ detailData.closedAt || '-' }}</p>
        <p>商品描述：</p>
        <div v-html="detailData.description || '无'" />
      </div>
      <template #footer>
        <el-button @click="detailDialog.show = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="orderDetailDialog.show" title="订单详情" width="600px">
      <div v-if="orderDetailData" style="line-height:2.2">
        <p>订单号：{{ orderDetailData.orderNo }}</p>
        <p>订单状态：<el-tag :type="statusType(orderDetailData.status)">{{ statusLabel(orderDetailData.status) }}</el-tag></p>
        <p>总金额：¥{{ orderDetailData.totalAmount }}</p>
        <p>下单时间：{{ orderDetailData.createdAt }}</p>
        <div v-if="orderDetailData.items && orderDetailData.items.length">
          <p>商品明细：</p>
          <div v-for="(item, idx) in orderDetailData.items" :key="idx" style="margin-left:20px">
            {{ item.productName }} × {{ item.quantity }} - ¥{{ item.unitPrice }}/件
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="orderDetailDialog.show = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 单个商品订单管理弹窗 -->
    <el-dialog v-model="productOrderDialog.show" :title="`商品订单管理`" width="85%">
      <OrderTable
        v-if="productOrderDialog.productId"
        :key="productOrderDialog.productId"
        ref="productOrderTableRef"
        :load-fn="(params) => loadProductOrders(productOrderDialog.productId, params)"
        role="seller"
        @view-detail="viewOrderDetail"
        @confirm-order="handleConfirmOrder"
        @stock-done="handleStockDone"
        @ship-order="handleShip"
        @complete-order="handleComplete"
        @cancel-order="handleCancelOrder"
      />
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * SellerProductList — 卖家「我的商品」页面
 *
 * 核心列表渲染委托给 SellerProductTable 业务组件，
 * 页面本身只负责弹窗交互与订单状态推进操作。
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SellerProductTable from '../../components/seller/SellerProductTable.vue'
import OrderTable from '../../components/order/OrderTable.vue'
import {
  getSellerProductDetail, updateStock, reCategory,
  offProduct, onProduct, setSalePrice,
  getProductOrders, confirmOrder, stockDoneOrder,
  shipOrder, completeOrder, cancelOrder,
  getSellerCategoryTree
} from '../../api/seller'

const router = useRouter()

// ─── 状态映射 ───
const statusLabel = (s) => ({
  CREATED: '客户下单', CONFIRMED: '商家确认', STOCKED: '备货完成',
  SHIPPING: '开始发货', COMPLETED: '交易完成', CANCELLED: '已取消'
}[s] || s)

const statusType = (s) => ({
  CREATED: '', CONFIRMED: 'warning', STOCKED: 'info',
  SHIPPING: 'primary', COMPLETED: 'success', CANCELLED: 'danger'
}[s] || '')

// ─── 子组件引用 ───
const productTableRef      = ref(null)
const productOrderTableRef = ref(null)

// ─── 分类数据（改挂分类弹窗用） ───
const flatCategory = ref([])
const loadCategories = async () => {
  try {
    const res = await getSellerCategoryTree()
    flatCategory.value = (res.data || []).flatMap(p => p.children || [])
  } catch { /* 兜底空列表 */ }
}
loadCategories()

// ─── 刷新商品列表 ───
const refreshList = () => productTableRef.value?.refresh()

// ═══════════════ 商品详情弹窗 ═══════════════
const detailDialog = reactive({ show: false })
const detailData   = ref(null)

const openDetail = async (id) => {
  try {
    const res = await getSellerProductDetail(id)
    detailData.value = res.data
    detailDialog.show = true
  } catch { ElMessage.error('获取商品详情失败') }
}

// ═══════════════ 修改库存弹窗 ═══════════════
const stockDialog = reactive({ show: false, id: null, stock: 0 })

const openStockDialog = (row) => {
  stockDialog.show = true
  stockDialog.id   = row.id
  stockDialog.stock = row.stock
}

const confirmStock = async () => {
  try {
    const res = await updateStock(stockDialog.id, stockDialog.stock)
    if (res?.code !== 200) {
      ElMessage.error(res?.msg || '库存修改失败')
      return
    }
    stockDialog.show = false
    ElMessage.success('库存已更新')
    refreshList()
  } catch { /* 拦截器已提示 */ }
}

// ═══════════════ 改挂分类弹窗 ═══════════════
const categoryDialog = reactive({ show: false, id: null, categoryId: null })

const openCategoryDialog = (row) => {
  categoryDialog.show = true
  categoryDialog.id   = row.id
  categoryDialog.categoryId = row.categoryId
}

const confirmCategory = async () => {
  if (!categoryDialog.categoryId) return ElMessage.warning('请选择目标分类')
  try {
    const res = await reCategory(categoryDialog.id, categoryDialog.categoryId)
    if (res?.code !== 200) {
      ElMessage.error(res?.msg || '分类修改失败')
      return
    }
    categoryDialog.show = false
    ElMessage.success('分类已更新')
    refreshList()
  } catch { /* 拦截器已提示 */ }
}

// ═══════════════ 设置活动价弹窗 ═══════════════
const saleDialog = reactive({
  show: false, id: null,
  salePrice: 0, saleStartAt: '', saleEndAt: ''
})

const openSaleDialog = (row) => {
  saleDialog.show = true
  saleDialog.id   = row.id
  saleDialog.salePrice  = row.salePrice || 0
  saleDialog.saleStartAt = row.saleStartAt || ''
  saleDialog.saleEndAt   = row.saleEndAt || ''
}

const confirmSale = async () => {
  if (saleDialog.salePrice <= 0) return ElMessage.warning('活动价必须大于 0')
  if (!saleDialog.saleStartAt || !saleDialog.saleEndAt) return ElMessage.warning('请设置起止时间')
  try {
    const res = await setSalePrice(saleDialog.id, {
      salePrice: saleDialog.salePrice,
      saleStartAt: saleDialog.saleStartAt,
      saleEndAt: saleDialog.saleEndAt
    })
    if (res?.code !== 200) {
      ElMessage.error(res?.msg || '活动价设置失败')
      return
    }
    saleDialog.show = false
    ElMessage.success('活动价已设置')
    refreshList()
  } catch { /* 拦截器已提示 */ }
}

// ═══════════════ 上 / 下架 ═══════════════
const handleOffline = async (id) => {
  try {
    const res = await offProduct(id)
    if (res?.code !== 200) {
      ElMessage.error(res?.msg || '下架失败')
      return
    }
    ElMessage.success('已下架')
    refreshList()
  } catch { /* 拦截器已提示 */ }
}

const handleOnline = async (id) => {
  try {
    const res = await onProduct(id)
    if (res?.code !== 200) {
      ElMessage.error(res?.msg || '上架失败')
      return
    }
    ElMessage.success('已上架')
    refreshList()
  } catch { /* 拦截器已提示 */ }
}

// ═══════════════ 单个商品订单管理弹窗 ═══════════════
const productOrderDialog = reactive({ show: false, productId: null })

const openProductOrderManage = (productId) => {
  productOrderDialog.show = true
  productOrderDialog.productId = productId
}

const loadProductOrders = async (productId, params) => {
  const res = await getProductOrders(productId, params)
  console.log('商品订单API返回:', res)
  return res.data || res
}

// ═══════════════ 订单详情弹窗 ═══════════════
const orderDetailDialog = reactive({ show: false })
const orderDetailData   = ref(null)

const viewOrderDetail = (order) => {
  orderDetailData.value = order
  orderDetailDialog.show = true
}

// ─── 订单状态推进 ───
const refreshOrderTable = () => productOrderTableRef.value?.refresh()

const handleConfirmOrder = async (row) => {
  await confirmOrder(row.id)
  ElMessage.success('已确认订单')
  refreshOrderTable()
}

const handleStockDone = async (row) => {
  await stockDoneOrder(row.id)
  ElMessage.success('已标记备货完成')
  refreshOrderTable()
}

const handleShip = async (row) => {
  await shipOrder(row.id)
  ElMessage.success('已开始发货')
  refreshOrderTable()
}

const handleComplete = async (row) => {
  await completeOrder(row.id)
  ElMessage.success('交易已完成')
  refreshOrderTable()
}

const handleCancelOrder = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？此操作不可逆。', '确认取消', { type: 'warning' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    refreshOrderTable()
  } catch { /* 用户取消弹窗 */ }
}
</script>
