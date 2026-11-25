<template>
  <div>
    <BizTable
      :data="list" :total="total"
      v-model:page="page" v-model:page-size="size"
      :loading="loading"
    >
      <!-- 展开行：申请详情（标题 / 描述 / 图片） -->
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding:8px 24px">
            <p><strong>问题描述：</strong>{{ row.description }}</p>
            <div v-if="row.images && row.images.length" style="display:flex;gap:8px;flex-wrap:wrap">
              <el-image
                v-for="(img, i) in row.images" :key="i" :src="img"
                :preview-src-list="row.images" :initial-index="i"
                style="width:90px;height:90px" fit="cover"
              />
            </div>
            <p class="muted">系统只记录申请与最终结果，协商在线下进行（不记录过程，FR-C13）。</p>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="售后单号" prop="afterSalesNo" min-width="140" />
      <el-table-column label="订单号" prop="orderNo" min-width="130" />
      <el-table-column label="商品" prop="productName" min-width="120" />
      <el-table-column label="标题" prop="title" min-width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="afterSalesStatusType(row.status)">{{ afterSalesStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.result" :type="afterSalesResultType(row.result)">{{ afterSalesResultLabel(row.result) }}</el-tag>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="createdAt" width="160" />

      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'APPLYING'" size="small"
            @click="handleNegotiating(row)"
          >标记协商中</el-button>
          <el-button
            v-if="row.status !== 'CLOSED'" size="small" type="primary"
            @click="openResultDialog(row)"
          >录入结果</el-button>
          <!-- 退货退款：确认收到后按钮置灰为「已收到退回件」，避免重复操作 -->
          <el-button
            v-if="row.status === 'CLOSED' && row.result === 'REFUND_AND_RETURN'"
            size="small" :type="isReturnReceived(row) ? 'info' : 'warning'"
            :disabled="isReturnReceived(row)"
            @click="openReturnDialog(row)"
          >{{ isReturnReceived(row) ? '已收到退回件' : '确认收到退回件' }}</el-button>
          <!-- 换货：登记完成后按钮变为「查看登记信息」，只读回显运单号 -->
          <el-button
            v-if="row.status === 'CLOSED' && row.result === 'EXCHANGE' && isExchangeRegistered(row)"
            size="small"
            @click="openExchangeViewDialog(row)"
          >查看登记信息</el-button>
          <el-button
            v-else-if="row.status === 'CLOSED' && row.result === 'EXCHANGE'"
            size="small" type="success"
            @click="openExchangeDialog(row)"
          >换货寄出登记</el-button>
        </template>
      </el-table-column>
    </BizTable>

    <!-- ① 录入售后结果 3.30 -->
    <el-dialog v-model="resultDialog.show" title="录入售后结果" width="520px">
      <p class="muted">售后单 {{ resultDialog.no }} —— 结果写入后闭环、不可再改（驳回后买家可重新发起）。</p>
      <el-form label-width="90px">
        <el-form-item label="售后结果">
          <el-radio-group v-model="resultDialog.result">
            <el-radio value="REFUND_AND_RETURN">退货退款</el-radio>
            <el-radio value="REFUND_ONLY">仅退款</el-radio>
            <el-radio value="EXCHANGE">换货</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结果说明">
          <el-input v-model="resultDialog.remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="选填" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon
        title="换货时若商品已下架或可卖量为 0，后端会自动转「仅退款」兜底；开始发货态退款将使订单转「已取消」。" />
      <template #footer>
        <el-button @click="resultDialog.show = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResult">确认录入</el-button>
      </template>
    </el-dialog>

    <!-- ② 确认收到退回件 3.31 -->
    <el-dialog v-model="returnDialog.show" title="确认收到退回件" width="480px">
      <p class="muted">退货退款：确认收到后同步执行「库存处理 + 全额原路退款」（原子、幂等）。</p>
      <el-form label-width="120px">
        <el-form-item label="寄回件运单号">
          <el-input v-model="returnDialog.returnWaybillNo" placeholder="选填（若有）" maxlength="40" />
          <p class="muted">建议填写：后端列表不返回退款状态，运单号是刷新页面后识别「已收到退回件」的依据。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialog.show = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReturn">确认收到并退款</el-button>
      </template>
    </el-dialog>

    <!-- ③ 换货寄出登记 3.32 -->
    <el-dialog v-model="exchangeDialog.show" title="换货寄出登记" width="480px">
      <p class="muted">1 换 1，账面库存不动；客户寄回旧件、卖家寄出新件，各记运单号。</p>
      <el-form label-width="130px">
        <el-form-item label="寄回旧件运单号">
          <el-input v-model="exchangeDialog.returnWaybillNo" maxlength="40" />
        </el-form-item>
        <el-form-item label="寄出新件运单号">
          <el-input v-model="exchangeDialog.exchangeWaybillNo" maxlength="40" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exchangeDialog.show = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitExchange">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- ④ 换货登记信息（已登记后只读回显） -->
    <el-dialog v-model="exchangeViewDialog.show" title="换货登记信息" width="480px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="售后单号">{{ exchangeViewDialog.no || '-' }}</el-descriptions-item>
        <el-descriptions-item label="寄回旧件运单号">{{ exchangeViewDialog.returnWaybillNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="寄出新件运单号">{{ exchangeViewDialog.exchangeWaybillNo || '-' }}</el-descriptions-item>
      </el-descriptions>
      <p class="muted" style="margin-top:12px">换货寄出已登记：1 换 1，账面库存不动。</p>
      <template #footer>
        <el-button @click="exchangeViewDialog.show = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * AfterSalesManageTable — 卖家售后处理业务组件（FR-C13~C16 / 3.28–3.32）
 *
 * 「列表三件套」封装售后单列表；行内按状态机提供：标记协商中 → 录入结果（四值）→
 * （退货退款）确认收到退回件 /（换货）换货寄出登记。全部接口调用内聚在组件内部，
 * 每次动作成功后自行刷新列表。
 *
 * 动作完成后的行内表现：退货退款已确认收到 → 灰色不可点的「已收到退回件」；
 * 换货已登记 → 变为「查看登记信息」，点开只读弹窗回显两个运单号。
 */
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getSellerAfterSalesList, markAfterSalesNegotiating, setAfterSalesResult,
  confirmReturnReceived, registerExchangeShip
} from '../../api/seller'
import {
  afterSalesStatusLabel, afterSalesStatusType,
  afterSalesResultLabel, afterSalesResultType
} from '../../utils/dict'
import BizTable from '../BizTable.vue'

const list    = ref([])
const total   = ref(0)
const loading = ref(false)
const page    = ref(1)
const size    = ref(10)
const submitting = ref(false)

const resultDialog   = reactive({ show: false, id: null, no: '', result: 'REFUND_AND_RETURN', remark: '' })
const returnDialog   = reactive({ show: false, id: null, returnWaybillNo: '' })
const exchangeDialog = reactive({ show: false, id: null, returnWaybillNo: '', exchangeWaybillNo: '' })
const exchangeViewDialog = reactive({ show: false, no: '', returnWaybillNo: '', exchangeWaybillNo: '' })

// 已完成动作的判定：
// 换货寄出运单号非空即已登记（登记时两个运单号均必填，信号来自后端，稳定可靠）；
// 退货退款的寄回运单号是选填，为空时后端无法告知是否已收到，故前端额外落盘记录已确认过的售后单 id。
const RETURN_RECEIVED_KEY = 'afterSalesReturnReceivedIds'
const loadReceivedIds = () => {
  try {
    return (localStorage.getItem(RETURN_RECEIVED_KEY) || '').split(',').filter(Boolean).map(Number)
  } catch { return [] }
}
const returnReceivedIds = ref(loadReceivedIds())
const markReturnReceived = (id) => {
  if (id == null || returnReceivedIds.value.includes(id)) return
  returnReceivedIds.value = [...returnReceivedIds.value, id]
  try { localStorage.setItem(RETURN_RECEIVED_KEY, returnReceivedIds.value.join(',')) } catch { /* 忽略存储异常 */ }
}
const isReturnReceived = (row) => !!row.returnWaybillNo || returnReceivedIds.value.includes(row.id)
const isExchangeRegistered = (row) => !!row.exchangeWaybillNo

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSellerAfterSalesList({ page: page.value, size: size.value })
    const data = res?.data || res
    list.value  = data?.list || []
    total.value = data?.total || 0
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const handleNegotiating = async (row) => {
  const res = await markAfterSalesNegotiating(row.id)
  if (res?.code === 200) { ElMessage.success('已标记为协商中'); loadData() }
  else ElMessage.error(res?.msg || '操作失败')
}

const openResultDialog = (row) => {
  Object.assign(resultDialog, { show: true, id: row.id, no: row.afterSalesNo, result: 'REFUND_AND_RETURN', remark: '' })
}
const submitResult = async () => {
  submitting.value = true
  try {
    const res = await setAfterSalesResult(resultDialog.id, { result: resultDialog.result, remark: resultDialog.remark })
    if (res?.code === 200) { ElMessage.success('结果已录入'); resultDialog.show = false; loadData() }
    else ElMessage.error(res?.msg || '录入失败')
  } finally { submitting.value = false }
}

const openReturnDialog = (row) => {
  Object.assign(returnDialog, { show: true, id: row.id, returnWaybillNo: row.returnWaybillNo || '' })
}
const submitReturn = async () => {
  submitting.value = true
  try {
    const res = await confirmReturnReceived(returnDialog.id, { returnWaybillNo: returnDialog.returnWaybillNo || undefined })
    if (res?.code === 200) {
      markReturnReceived(returnDialog.id)
      ElMessage.success('已确认收到退回件并完成退款'); returnDialog.show = false; loadData()
    }
    else if ((res?.msg || '').includes('已确认收货')) {
      // 后端幂等拦截同样说明该单已退款完成：补记后按钮置灰，避免可重复点击
      markReturnReceived(returnDialog.id)
      ElMessage.warning('该售后单此前已确认收到退回件'); returnDialog.show = false; loadData()
    }
    else ElMessage.error(res?.msg || '操作失败')
  } finally { submitting.value = false }
}

const openExchangeDialog = (row) => {
  Object.assign(exchangeDialog, {
    show: true, id: row.id,
    returnWaybillNo: row.returnWaybillNo || '',
    exchangeWaybillNo: row.exchangeWaybillNo || ''
  })
}
const openExchangeViewDialog = (row) => {
  Object.assign(exchangeViewDialog, {
    show: true, no: row.afterSalesNo,
    returnWaybillNo: row.returnWaybillNo || '',
    exchangeWaybillNo: row.exchangeWaybillNo || ''
  })
}
const submitExchange = async () => {
  if (!exchangeDialog.returnWaybillNo || !exchangeDialog.exchangeWaybillNo) {
    return ElMessage.warning('两个运单号均不能为空')
  }
  submitting.value = true
  try {
    const res = await registerExchangeShip(exchangeDialog.id, {
      returnWaybillNo: exchangeDialog.returnWaybillNo,
      exchangeWaybillNo: exchangeDialog.exchangeWaybillNo
    })
    if (res?.code === 200) { ElMessage.success('换货寄出已登记'); exchangeDialog.show = false; loadData() }
    else ElMessage.error(res?.msg || '登记失败')
  } finally { submitting.value = false }
}

watch([page, size], () => loadData())
onMounted(() => loadData())
defineExpose({ refresh: loadData })
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
