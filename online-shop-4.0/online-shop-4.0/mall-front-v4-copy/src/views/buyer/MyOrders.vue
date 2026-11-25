<template>
  <div style="max-width:1200px;margin:30px auto;padding:0 20px">
    <div class="orders-head">
      <h2>我的订单</h2>
      <div>
        <el-button @click="$router.push('/buyer/address')">收货地址</el-button>
        <el-button @click="$router.push('/buyer/after-sales')">我的售后</el-button>
      </div>
    </div>

    <!-- 订单列表业务组件（买家视图，v4.0） -->
    <OrderTable
      ref="orderTableRef"
      :load-fn="loadOrders"
      role="buyer"
      @view-detail="viewOrderDetail"
      @pay="openPay"
      @confirm-receipt="handleConfirmReceipt"
      @view-shipping="openShipping"
      @after-sales="openAfterSales"
      @review="openReview"
      @cancel-order="handleCancel"
    />

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialog.show" title="订单详情" width="640px">
      <div v-if="detailData" style="line-height:2">
        <p>订单号：{{ detailData.orderNo }}</p>
        <p>订单状态：<el-tag :type="orderStatusType(detailData.status)">{{ orderStatusLabel(detailData.status) }}</el-tag></p>
        <p>总金额：¥{{ detailData.totalAmount }}</p>
        <p>支付渠道：{{ detailData.paymentChannel ? payChannelLabel(detailData.paymentChannel) : '未支付' }}　支付时间：{{ detailData.paidAt || '-' }}</p>
        <p>收货地址：{{ formatAddress(detailData.addressSnapshot) }}</p>
        <p>物流：{{ detailData.logisticsProvider || '-' }}　运单号：{{ detailData.waybillNo || '-' }}</p>
        <p>下单时间：{{ detailData.createdAt }}</p>
        <el-divider style="margin:8px 0" />
        <p>商品明细：</p>
        <div v-for="item in (detailData.items || [])" :key="item.id" class="detail-item">
          <span>{{ item.productName }} × {{ item.quantity }} - ¥{{ item.unitPrice }}/件</span>
          <span class="muted">
            <el-tag v-if="item.reviewed" size="small" type="success">已评价</el-tag>
            <el-tag v-else-if="item.refundClosed" size="small" type="info">已退款</el-tag>
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.show = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 支付弹窗（2.19 + 4.1） -->
    <PayDialog v-model:show="payDialog.show" :order="payDialog.order" @paid="refresh" />
    <!-- 物流查询弹窗（2.26） -->
    <LogisticsDialog v-model:show="shipDialog.show" :order="shipDialog.order" />
    <!-- 发起售后弹窗（2.27） -->
    <AfterSalesForm v-model:show="asDialog.show" :order="asDialog.order" @submitted="refresh" />
    <!-- 发表评价弹窗（2.29） -->
    <ReviewForm v-model:show="reviewDialog.show" :order="reviewDialog.order" @submitted="refresh" />
  </div>
</template>

<script setup>
/**
 * MyOrders — 买家订单历史页面（v4.0）
 *
 * 页面职责：组合 OrderTable 业务组件与其派生的动作弹窗（支付 / 物流 / 售后 / 评价），
 * 处理确认收货、取消等需二次确认的动作，并在动作成功后 refresh 列表。
 * 组件间通信统一采用「props 下传 + emit 上报 + ref 调用 refresh」，不引入全局事件总线。
 */
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBuyerOrderHistory, cancelBuyerOrder, confirmReceipt } from '../../api/buyer'
import { orderStatusLabel, orderStatusType, payChannelLabel, formatAddress } from '../../utils/dict'
import OrderTable from '../../components/order/OrderTable.vue'
import PayDialog from '../../components/buyer/PayDialog.vue'
import LogisticsDialog from '../../components/buyer/LogisticsDialog.vue'
import AfterSalesForm from '../../components/buyer/AfterSalesForm.vue'
import ReviewForm from '../../components/buyer/ReviewForm.vue'

const orderTableRef = ref(null)
const refresh = () => orderTableRef.value?.refresh()

const loadOrders = async (params) => {
  const res = await getBuyerOrderHistory(params)
  return res.data || res
}

// ─── 详情弹窗 ───
const detailDialog = reactive({ show: false })
const detailData   = ref(null)
const viewOrderDetail = (order) => { detailData.value = order; detailDialog.show = true }

// ─── 支付 ───
const payDialog = reactive({ show: false, order: null })
const openPay = (order) => { payDialog.order = order; payDialog.show = true }

// ─── 物流查询 ───
const shipDialog = reactive({ show: false, order: null })
const openShipping = (order) => { shipDialog.order = order; shipDialog.show = true }

// ─── 售后申请 ───
const asDialog = reactive({ show: false, order: null })
const openAfterSales = (order) => { asDialog.order = order; asDialog.show = true }

// ─── 评价 ───
const reviewDialog = reactive({ show: false, order: null })
const openReview = (order) => { reviewDialog.order = order; reviewDialog.show = true }

// ─── 确认收货（触发预占转正式扣减） ───
const handleConfirmReceipt = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确认已收到商品？确认后订单将交易完成并扣减库存，不可撤销。',
      '确认收货', { type: 'warning', confirmButtonText: '确认收货', cancelButtonText: '再看看' }
    )
    const res = await confirmReceipt(row.id)
    if (res?.code === 200) { ElMessage.success('已确认收货，交易完成'); refresh() }
    else ElMessage.error(res?.msg || '操作失败')
  } catch { /* 用户取消 */ }
}

// ─── 取消订单（已支付段取消将原路退款） ───
const handleCancel = async (row) => {
  const paidTip = ['PAID', 'CONFIRMED', 'STOCKED'].includes(row.status)
    ? '该订单已支付，取消将全额原路退款。' : ''
  try {
    await ElMessageBox.confirm(
      `确定取消该订单吗？取消后不可恢复。${paidTip}`,
      '确认取消订单', { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
    const res = await cancelBuyerOrder(row.id)
    if (res?.code === 200) {
      ElMessage.success(paidTip ? '订单已取消，退款将原路退回' : '订单已取消')
      refresh()
    } else ElMessage.error(res?.msg || '取消失败')
  } catch { /* 用户取消 */ }
}
</script>

<style scoped>
.orders-head { display: flex; justify-content: space-between; align-items: center; }
.detail-item { display: flex; justify-content: space-between; margin-left: 16px; }
.muted { color: #909399; }
</style>
