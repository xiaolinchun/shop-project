<template>
  <el-dialog
    :model-value="show" title="物流状态" width="560px"
    @update:model-value="v => emit('update:show', v)"
  >
    <div v-loading="loading" style="min-height:120px">
      <template v-if="data">
        <el-descriptions :column="1" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="订单号">{{ data.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="物流服务商">{{ data.logisticsProvider || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ data.waybillNo || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="data.tracking && data.tracking.length">
          <el-timeline>
            <el-timeline-item
              v-for="(t, i) in data.tracking" :key="i"
              :timestamp="t.time" placement="top"
              :type="i === 0 ? 'primary' : ''"
            >
              <strong>{{ trackingStatusLabel(t.status) }}</strong>
              <div class="muted">{{ t.description }}</div>
            </el-timeline-item>
          </el-timeline>
          <p class="muted">「已签收」仅作跟踪展示，不会自动完成订单，需买家手动确认收货。</p>
        </div>
        <el-empty v-else :image-size="60" description="暂无跟踪信息" />
      </template>
      <el-alert v-else-if="!loading" :title="emptyMsg" type="info" :closable="false" />
    </div>

    <template #footer>
      <el-button @click="emit('update:show', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * LogisticsDialog — 买家物流状态查询业务组件（FR-C11 / 2.26）
 *
 * 打开时按 orderId 拉取运单号 + 跟踪信息条目（时间倒序）。
 * 「已签收」仅展示，不自动推进订单状态（CA-C5）。
 */
import { ref, watch } from 'vue'
import { getOrderShipping } from '../../api/buyer'
import { trackingStatusLabel } from '../../utils/dict'

const props = defineProps({
  show:  { type: Boolean, default: false },
  order: { type: Object,  default: null }
})
const emit = defineEmits(['update:show'])

const data      = ref(null)
const loading   = ref(false)
const emptyMsg  = ref('订单尚未发货')

const load = async () => {
  if (!props.order?.id) return
  loading.value = true
  data.value = null
  try {
    const res = await getOrderShipping(props.order.id)
    if (res?.code === 200 && res.data) {
      data.value = res.data
    } else {
      emptyMsg.value = res?.msg || '订单尚未发货'
    }
  } catch (e) {
    emptyMsg.value = e?.response?.data?.msg || '订单尚未发货'
  }
  loading.value = false
}

watch(() => props.show, (v) => { if (v) load() })
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
