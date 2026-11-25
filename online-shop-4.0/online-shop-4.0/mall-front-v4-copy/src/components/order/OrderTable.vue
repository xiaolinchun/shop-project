<template>
  <div>
    <BizTable
      :data="list" :total="total"
      v-model:page="page" v-model:page-size="size"
      :loading="loading"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="order-expand">
            <div class="order-expand__block">
              <div class="order-expand__title">商品明细</div>
              <div v-for="item in (row.items || [])" :key="item.id" class="order-expand__item">
                <span>{{ item.productName }} × {{ item.quantity }}</span>
                <span class="muted">¥{{ item.unitPrice }}/件</span>
              </div>
            </div>
            <div class="order-expand__block">
              <div class="order-expand__title">收货地址</div>
              <div class="order-expand__line">{{ formatAddress(row.addressSnapshot) }}</div>
              <div class="order-expand__title" style="margin-top:8px">支付信息</div>
              <div class="order-expand__line">
                渠道：{{ payChannelLabel(row.paymentChannel) }}　支付时间：{{ row.paidAt || '-' }}
              </div>
              <div class="order-expand__title" style="margin-top:8px">物流信息</div>
              <div class="order-expand__line">
                {{ row.logisticsProvider || '-' }}　运单号：{{ row.waybillNo || '-' }}
              </div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="订单号" prop="orderNo" min-width="150" />

      <!-- 商品明细（简略展示，完整可展开行查看） -->
      <el-table-column label="商品" min-width="180">
        <template #default="{ row }">
          <div v-for="item in (row.items || []).slice(0, 2)" :key="item.id" style="font-size:13px">
            {{ item.productName }} × {{ item.quantity }}
          </div>
          <span v-if="(row.items || []).length > 2" class="muted">等 {{ row.items.length }} 件</span>
        </template>
      </el-table-column>

      <el-table-column label="总额" width="90">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>

      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="支付渠道" width="100">
        <template #default="{ row }">{{ row.paymentChannel ? payChannelLabel(row.paymentChannel) : '-' }}</template>
      </el-table-column>

      <el-table-column label="物流" width="150">
        <template #default="{ row }">
          <span v-if="row.waybillNo">{{ row.logisticsProvider }}<br><span class="muted">{{ row.waybillNo }}</span></span>
          <span v-else class="muted">未发货</span>
        </template>
      </el-table-column>

      <!-- 卖家视图：预占 / 可卖 / 停留天数（3.12 卡单标记废弃后改为展示口径） -->
      <template v-if="role === 'seller'">
        <el-table-column label="预占" width="70">
          <template #default="{ row }">{{ row.reservedQuantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="可卖量" width="80">
          <template #default="{ row }">
            <span :style="row.availableQuantity != null && row.availableQuantity <= 0 ? 'color:#f56c6c;font-weight:bold' : ''">
              {{ row.availableQuantity ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="停留天数" width="90">
          <template #default="{ row }">{{ row.stayDays ?? '-' }}</template>
        </el-table-column>
      </template>

      <el-table-column label="下单时间" prop="createdAt" width="165" />

      <!-- 操作列 -->
      <el-table-column label="操作" :width="role === 'buyer' ? 360 : 260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('view-detail', row)">详情</el-button>

          <!-- 买家操作（v4.0 支付 / 确认收货 / 物流 / 售后 / 评价） -->
          <template v-if="role === 'buyer'">
            <el-button
              v-if="canPay(row)" size="small" type="warning"
              @click="$emit('pay', row)"
            >去支付</el-button>
            <el-button
              v-if="canConfirmReceipt(row)" size="small" type="success"
              @click="$emit('confirm-receipt', row)"
            >确认收货</el-button>
            <el-button
              v-if="shippingQuery(row)" size="small"
              @click="$emit('view-shipping', row)"
            >查物流</el-button>
            <el-button
              v-if="canAfterSales(row)" size="small" type="info"
              @click="$emit('after-sales', row)"
            >申请售后</el-button>
            <el-button
              v-if="canReview(row)" size="small" type="primary" plain
              @click="$emit('review', row)"
            >评价</el-button>
            <el-button
              v-if="buyerCanCancel(row)" size="small" type="danger"
              @click="$emit('cancel-order', row)"
            >取消订单</el-button>
          </template>

          <!-- 卖家操作（严格按 7 态推进；交易完成改由买家确认收货，3.16 已舍弃） -->
          <template v-else>
            <el-button
              v-if="row.status === 'PAID'" size="small" type="primary"
              @click="$emit('confirm-order', row)"
            >确认订单</el-button>
            <el-button
              v-if="row.status === 'CONFIRMED'" size="small" type="primary"
              @click="$emit('stock-done', row)"
            >备货完成</el-button>
            <el-button
              v-if="row.status === 'STOCKED'" size="small" type="primary"
              @click="$emit('ship-order', row)"
            >开始发货</el-button>
            <el-button
              v-if="!['COMPLETED','CANCELLED'].includes(row.status)"
              size="small" type="danger"
              @click="$emit('cancel-order', row)"
            >取消订单</el-button>
          </template>
        </template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * OrderTable — 订单列表业务组件（买家 / 卖家通用，v4.0 升级版）
 *
 * 在 BizTable（表格 + 分页）之上封装 v4.0 的 7 态状态机与全部行级入口，
 * 通过 role 区分买家 / 卖家视图：
 *   - 买家：去支付 / 确认收货 / 查物流 / 申请售后 / 评价 / 取消（按入口标记或状态兜底）
 *   - 卖家：确认订单(仅 PAID) / 备货完成 / 开始发货(登记运单) / 取消，并展示预占·可卖·停留
 *
 * 所有行级动作通过 emit 交给父页面（父页面组合弹窗与 API 调用），
 * 组件本身只负责数据加载、分页、状态渲染，是「列表三件套」的业务化封装。
 */
import { ref, watch, onMounted } from 'vue'
import BizTable from '../BizTable.vue'
import {
  orderStatusLabel, orderStatusType, payChannelLabel, formatAddress,
  BUYER_CANCELLABLE, AFTER_SALES_WINDOW
} from '../../utils/dict'

const props = defineProps({
  /** 数据加载函数，接收 { page, size }，返回 { list, total } 或 { data:{list,total} } */
  loadFn: { type: Function, required: true },
  /** 视图角色：'buyer' | 'seller' */
  role:   { type: String, default: 'buyer' }
})

const emit = defineEmits([
  'view-detail',
  // 买家
  'pay', 'confirm-receipt', 'view-shipping', 'after-sales', 'review', 'cancel-order',
  // 卖家
  'confirm-order', 'stock-done', 'ship-order'
])

const list    = ref([])
const total   = ref(0)
const loading = ref(false)
const page    = ref(1)
const size    = ref(10)

// ─── 入口可见性：优先用后端回传的 canXxx 标记，缺失时按状态兜底 ───
const canPay            = (r) => r.canPay            ?? (r.status === 'PENDING_PAYMENT')
const canConfirmReceipt = (r) => r.canConfirmReceipt ?? (r.status === 'SHIPPING')
const shippingQuery     = (r) => r.shippingQuery     ?? (['SHIPPING', 'COMPLETED'].includes(r.status) && !!r.waybillNo)
const canAfterSales     = (r) => r.canAfterSales     ?? AFTER_SALES_WINDOW.includes(r.status)
const canReview         = (r) => r.canReview         ?? (r.status === 'COMPLETED')
const buyerCanCancel    = (r) => BUYER_CANCELLABLE.includes(r.status)

// ─── 数据加载 ───
const loadData = async () => {
  loading.value = true
  try {
    const res = await props.loadFn({ page: page.value, size: size.value })
    if (res?.list) {
      list.value  = res.list
      total.value = res.total || 0
    } else if (res?.data) {
      list.value  = res.data.list || []
      total.value = res.data.total || 0
    } else {
      list.value = []; total.value = 0
    }
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

watch([page, size], () => loadData())
onMounted(() => loadData())
defineExpose({ refresh: loadData })
</script>

<style scoped>
.order-expand {
  display: flex;
  gap: 40px;
  padding: 8px 24px;
  flex-wrap: wrap;
}
.order-expand__title { font-weight: bold; margin-bottom: 4px; color: #606266; }
.order-expand__item  { font-size: 13px; line-height: 1.8; display: flex; gap: 12px; }
.order-expand__line  { font-size: 13px; color: #606266; }
.muted { color: #909399; font-size: 12px; }
</style>
