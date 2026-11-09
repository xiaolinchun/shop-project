<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>全部客户列表</h2>

    <!-- 客户列表业务组件 -->
    <CustomerTable @view-history="openHistory" />

    <!-- 客户购买历史弹窗（订单维度，D-B01 行内展开） -->
    <el-dialog v-model="historyDialog.show" :title="`客户「${historyDialog.username}」的购买历史`" width="85%">
      <OrderTable
        v-if="historyDialog.username"
        :key="historyDialog.username"
        ref="orderTableRef"
        :load-fn="(params) => loadHistory(historyDialog.username, params)"
        role="seller"
        @view-detail="viewOrderDetail"
        @confirm-order="handleConfirmOrder"
        @stock-done="handleStockDone"
        @ship-order="handleShip"
        @complete-order="handleComplete"
        @cancel-order="handleCancelOrder"
      />
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialog.show" title="订单详情" width="600px">
      <div v-if="detailData" style="line-height:2.2">
        <p>订单号：{{ detailData.orderNo }}</p>
        <p>订单状态：<el-tag :type="statusType(detailData.status)">{{ statusLabel(detailData.status) }}</el-tag></p>
        <p>总金额：¥{{ detailData.totalAmount }}</p>
        <p>下单时间：{{ detailData.createdAt }}</p>
        <div v-if="detailData.items && detailData.items.length">
          <p>商品明细：</p>
          <div v-for="(item, idx) in detailData.items" :key="idx" style="margin-left:20px">
            {{ item.productName }} × {{ item.quantity }} - ¥{{ item.unitPrice }}/件
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.show = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * CustomerList — 卖家客户列表页面
 *
 * 客户列表委托给 CustomerTable 业务组件，
 * 购买历史弹窗内嵌 OrderTable 订单业务组件。
 */
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomerHistory, confirmOrder, stockDoneOrder, shipOrder, completeOrder, cancelOrder } from '../../api/seller'
import CustomerTable from '../../components/customer/CustomerTable.vue'
import OrderTable from '../../components/order/OrderTable.vue'

// ─── 子组件引用 ───
const orderTableRef = ref(null)

// ─── 状态映射 ───
const statusLabel = (s) => ({
  CREATED: '客户下单', CONFIRMED: '商家确认', STOCKED: '备货完成',
  SHIPPING: '开始发货', COMPLETED: '交易完成', CANCELLED: '已取消'
}[s] || s)

const statusType = (s) => ({
  CREATED: '', CONFIRMED: 'warning', STOCKED: 'info',
  SHIPPING: 'primary', COMPLETED: 'success', CANCELLED: 'danger'
}[s] || '')

// ─── 购买历史弹窗 ───
const historyDialog = reactive({ show: false, username: '' })

const openHistory = (username) => {
  historyDialog.show = true
  historyDialog.username = username
}

const loadHistory = async (username, params) => {
  const res = await getCustomerHistory(username, params)
  return res.data || res
}

// ─── 刷新订单列表 ───
const refreshOrders = () => orderTableRef.value?.refresh()

// ═══════════════ 订单详情弹窗 ═══════════════
const detailDialog = reactive({ show: false })
const detailData   = ref(null)

const viewOrderDetail = (order) => {
  detailData.value = order
  detailDialog.show = true
}

// ═══════════════ 订单状态推进 ═══════════════
const handleConfirmOrder = async (row) => {
  try {
    await confirmOrder(row.id)
    ElMessage.success('已确认订单')
    refreshOrders()
  } catch { /* 拦截器已提示 */ }
}

const handleStockDone = async (row) => {
  try {
    await stockDoneOrder(row.id)
    ElMessage.success('已标记备货完成')
    refreshOrders()
  } catch { /* 拦截器已提示 */ }
}

const handleShip = async (row) => {
  try {
    await shipOrder(row.id)
    ElMessage.success('已开始发货')
    refreshOrders()
  } catch { /* 拦截器已提示 */ }
}

const handleComplete = async (row) => {
  try {
    await completeOrder(row.id)
    ElMessage.success('交易已完成')
    refreshOrders()
  } catch { /* 拦截器已提示 */ }
}

const handleCancelOrder = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？此操作不可逆。', '确认取消', { type: 'warning' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    refreshOrders()
  } catch { /* 用户取消弹窗 */ }
}
</script>
