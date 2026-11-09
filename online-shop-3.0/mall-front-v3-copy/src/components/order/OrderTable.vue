<template>
  <div>
    <BizTable
      :data="list" :total="total"
      v-model:page="page" v-model:page-size="size"
      :loading="loading"
    >
      <!-- 卡单标记列（仅卖家视图显示） -->
      <el-table-column v-if="role === 'seller'" label="异常" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.shipBlocked" type="danger" size="small">
            无法发货（库存不足）
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="订单号" prop="orderNo" min-width="160" />

      <!-- 商品明细（行内展开，D-B01） -->
      <el-table-column label="商品明细" min-width="260">
        <template #default="{ row }">
          <div v-for="(item, idx) in (row.items || [])" :key="idx" style="font-size:13px">
            {{ item.productName }} × {{ item.quantity }}
            <span style="color:#999">（¥{{ item.unitPrice }}/件）</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="总金额" width="100">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>

      <el-table-column label="订单状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="下单时间" prop="createdAt" width="170" />

      <!-- 卖家视图：缺口可视化 -->
      <el-table-column v-if="role === 'seller'" label="库存缺口" width="180">
        <template #default="{ row }">
          <span v-if="row.needQty != null">
            需要 {{ row.needQty }} / 当前 {{ row.currentStock }} / 已发货 {{ row.shippedQty }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>

      <!-- 卖家视图：停留天数 -->
      <el-table-column v-if="role === 'seller'" label="停留天数" width="100">
        <template #default="{ row }">
          {{ row.stayDays ?? '-' }}
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('view-detail', row)">详情</el-button>

          <!-- 卖家操作按钮（按状态机严格推进） -->
          <template v-if="role === 'seller'">
            <el-button
              v-if="row.status === 'CREATED'" size="small" type="primary"
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
              v-if="row.status === 'SHIPPING'" size="small" type="success"
              @click="$emit('complete-order', row)"
            >交易完成</el-button>
            <el-button
              v-if="row.status !== 'COMPLETED' && row.status !== 'CANCELLED'"
              size="small" type="danger"
              @click="$emit('cancel-order', row)"
            >取消订单</el-button>
          </template>

          <!-- 买家操作按钮 -->
          <template v-if="role === 'buyer'">
            <el-button
              v-if="['CREATED','CONFIRMED','STOCKED'].includes(row.status)"
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
 * OrderTable — 订单列表业务组件（买家 / 卖家通用）
 *
 * 通过 role prop 区分买家视图与卖家视图：
 *   - 卖家视图额外显示「异常标记」「库存缺口」「停留天数」列
 *   - 买家视图仅显示基础订单信息 + 取消操作
 *
 * 所有行级操作通过 emit 暴露给父页面。
 */
import { ref, watch, onMounted } from 'vue'
import BizTable from '../BizTable.vue'

const props = defineProps({
  /** 数据加载函数，接收 { page, size }，返回 { list, total } */
  loadFn:  { type: Function, required: true },
  /** 视图角色：'buyer' | 'seller' */
  role:    { type: String, default: 'buyer' }
})

const emit = defineEmits([
  'view-detail', 'confirm-order', 'stock-done',
  'ship-order', 'complete-order', 'cancel-order'
])

const list    = ref([])
const total   = ref(0)
const loading = ref(false)
const page    = ref(1)
const size    = ref(10)

// ─── 状态映射 ───
const statusLabel = (s) => ({
  CREATED: '客户下单', CONFIRMED: '商家确认', STOCKED: '备货完成',
  SHIPPING: '开始发货', COMPLETED: '交易完成', CANCELLED: '已取消'
}[s] || s)

const statusType = (s) => ({
  CREATED: '', CONFIRMED: 'warning', STOCKED: 'info',
  SHIPPING: 'primary', COMPLETED: 'success', CANCELLED: 'danger'
}[s] || '')

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

// 分页变化时自动重新加载
watch([page, size], () => loadData())

onMounted(() => loadData())
defineExpose({ refresh: loadData })
</script>
