<template>
  <el-dialog
    :model-value="show" :title="`订单支付`" width="520px"
    @update:model-value="v => emit('update:show', v)"
    @close="onClose"
  >
    <div v-if="order" class="pay">
      <!-- 金额 -->
      <div class="pay__amount">
        <div class="muted">订单号 {{ order.orderNo }}</div>
        <div>应付金额：<span class="pay__price">¥{{ order.totalAmount }}</span></div>
        <div class="muted">待支付倒计时 24 小时内完成，超时自动取消</div>
      </div>

      <!-- ① 选择渠道（4 渠道全支持 FR-C02） -->
      <div class="pay__section-title">选择支付方式</div>
      <div class="pay__channels">
        <div
          v-for="c in PAY_CHANNELS" :key="c.value"
          class="pay__channel" :class="{ 'pay__channel--active': channel === c.value }"
          :style="{ borderColor: channel === c.value ? c.color : '' }"
          @click="channel = c.value"
        >
          <el-icon :size="26" :color="c.color"><component :is="c.icon" /></el-icon>
          <span>{{ c.label }}</span>
        </div>
      </div>

      <!-- ② 模拟渠道控制项（附录 A.2，仅课程演示用） -->
      <el-collapse v-model="mockPanel" class="pay__mock">
        <el-collapse-item title="模拟渠道选项（技术实现层 · 演示用）" name="mock">
          <el-checkbox v-model="mockOpts.fail">模拟支付失败 / 放弃</el-checkbox>
          <div class="muted" style="margin-top:6px">
            勾选「模拟失败」将以 FAIL 回调，订单停留待支付、可重试；否则以 SUCCESS 回调，
            后端原子执行「可卖量校验 + 预占」。
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-alert
        v-if="resultText" :title="resultText" :type="resultType"
        show-icon :closable="false" style="margin-top:12px"
      />
    </div>

    <template #footer>
      <el-button @click="emit('update:show', false)">取消支付</el-button>
      <el-button type="primary" :loading="paying" :disabled="!channel" @click="handlePay">
        {{ mockOpts.fail ? '模拟失败并回调' : `确认支付${channelLabel ? '（' + channelLabel + '）' : ''}` }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * PayDialog — 买家支付业务组件（FR-C02 / C03 / 2.19 + 4.1 + 附录 A）
 *
 * 内聚「选渠道 → 2.19 发起支付拿 paymentNo → 模拟渠道 → 4.1 回调」全链路：
 *   - SUCCESS 回调：后端原子执行「可卖量校验 + 预占」，可卖量足 → 订单转 PAID
 *   - 渠道已扣款但可卖量不足：后端自动全额原路退款、订单停留待支付（不资损）
 *   - FAIL / 放弃：订单停留 PENDING_PAYMENT，可重试（幂等由后端保证）
 * 组件不直接改订单列表，成功后 emit('paid')，由父页面刷新。
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { payOrder } from '../../api/buyer'
import { paymentNotify, PAY_CHANNELS } from '../../api/payment'

const props = defineProps({
  show:  { type: Boolean, default: false },
  order: { type: Object,  default: null }   // { id, orderNo, totalAmount }
})
const emit = defineEmits(['update:show', 'paid'])

const channel   = ref('')
const paying    = ref(false)
const mockPanel = ref([])
const mockOpts  = reactive({ fail: false })
const resultText = ref('')
const resultType = ref('info')

const channelLabel = computed(() => PAY_CHANNELS.find(c => c.value === channel.value)?.label || '')

const pad = (n) => String(n).padStart(2, '0')
const nowStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
const genTxnNo = () => `MOCK${Date.now()}${Math.floor(Math.random() * 1000)}`

const handlePay = async () => {
  if (!channel.value) return ElMessage.warning('请选择支付渠道')
  paying.value = true
  resultText.value = ''
  try {
    // ① 发起支付，生成支付单（PROCESSING）
    const payRes = await payOrder(props.order.id, channel.value)
    if (payRes?.code !== 200 || !payRes.data?.paymentNo) {
      resultType.value = 'error'
      resultText.value = payRes?.msg || '发起支付失败'
      return
    }
    const { paymentNo, amount } = payRes.data

    // ② 模拟渠道回调（4.1）
    const notifyRes = await paymentNotify({
      paymentNo,
      channel: channel.value,
      channelTxnNo: genTxnNo(),
      result: mockOpts.fail ? 'FAIL' : 'SUCCESS',
      paidAt: mockOpts.fail ? undefined : nowStr(),
      amount
    })

    if (notifyRes?.code !== 200) {
      resultType.value = 'error'
      resultText.value = notifyRes?.msg || '支付回调异常'
      return
    }
    const status = notifyRes.data?.status
    if (status === 'PAID') {
      resultType.value = 'success'
      resultText.value = '支付成功，订单已进入「已支付」'
      ElMessage.success('支付成功')
      setTimeout(() => { emit('paid'); emit('update:show', false) }, 700)
    } else if (mockOpts.fail) {
      resultType.value = 'warning'
      resultText.value = '已模拟支付失败，订单停留待支付，可重试'
    } else {
      // SUCCESS 回调但状态仍为待支付：渠道已扣款而可卖量不足，已自动原路退款
      resultType.value = 'warning'
      resultText.value = notifyRes.msg || '可卖量不足，已全额原路退款，订单停留待支付'
    }
  } catch (e) {
    resultType.value = 'error'
    resultText.value = e?.response?.data?.msg || '支付请求失败'
  } finally {
    paying.value = false
  }
}

const onClose = () => {
  channel.value = ''
  mockOpts.fail = false
  resultText.value = ''
}
</script>

<style scoped>
.pay__amount { background: #f7f8fa; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; }
.pay__price  { font-size: 22px; font-weight: bold; color: #e6a23c; }
.pay__section-title { font-weight: bold; margin-bottom: 8px; }
.pay__channels { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.pay__channel {
  border: 2px solid #e4e7ed; border-radius: 8px; padding: 12px 4px;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  cursor: pointer; font-size: 13px; transition: all 0.2s;
}
.pay__channel--active { box-shadow: 0 0 0 2px rgba(0,0,0,0.03); }
.pay__mock { margin-top: 12px; }
.muted { color: #909399; font-size: 12px; }
</style>
