<template>
  <div style="max-width:1100px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>我的订单</h2>

    <!-- 订单列表业务组件（买家视图） -->
    <OrderTable
      ref="orderTableRef"
      :load-fn="loadOrders"
      role="buyer"
      @view-detail="viewOrderDetail"
      @cancel-order="handleCancel"
    />

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
 * MyOrders — 买家订单历史页面
 *
 * 使用 OrderTable 业务组件（role='buyer'），
 * 支持行内展开明细（D-B01）和取消订单操作（FR-B04）。
 */
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBuyerOrderHistory, cancelBuyerOrder } from '../../api/buyer'
import OrderTable from '../../components/order/OrderTable.vue'

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

// ─── 加载订单数据 ───
const loadOrders = async (params) => {
  const res = await getBuyerOrderHistory(params)
  return res.data || res
}

// ═══════════════ 订单详情弹窗 ═══════════════
const detailDialog = reactive({ show: false })
const detailData   = ref(null)

const viewOrderDetail = (order) => {
  detailData.value = order
  detailDialog.show = true
}

// ─── 取消订单 ───
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定取消该订单吗？取消后不可恢复。',
      '确认取消订单',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
    await cancelBuyerOrder(row.id)
    ElMessage.success('订单已取消')
    orderTableRef.value?.refresh()
  } catch { /* 用户取消弹窗 */ }
}
</script>
