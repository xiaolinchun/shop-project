<template>
  <el-dialog
    :model-value="show" title="开始发货 · 登记运单" width="480px"
    @update:model-value="v => emit('update:show', v)"
    @open="loadProviders"
  >
    <div v-if="order" class="muted" style="margin-bottom:12px">
      订单号 {{ order.orderNo }}
      <span v-if="order.items">　·　{{ order.items.map(i => `${i.productName}×${i.quantity}`).join('，') }}</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="物流服务商" prop="logisticsProviderId">
        <el-select v-model="form.logisticsProviderId" placeholder="从清单选择" style="width:100%" :loading="loadingProviders">
          <el-option v-for="p in providers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <div v-if="!loadingProviders && !providers.length" class="muted" style="margin-top:4px">
          还没有服务商，请先前往「物流管理」新增。
        </div>
      </el-form-item>
      <el-form-item label="运单号" prop="waybillNo">
        <el-input v-model="form.waybillNo" placeholder="一个订单一个运单，不拆包裹" maxlength="40" />
      </el-form-item>
    </el-form>

    <p class="muted">
      v4.0 发货不再做容量校验，凭预占放行；登记成功即置「开始发货」，账面库存不变。
    </p>

    <template #footer>
      <el-button @click="emit('update:show', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleShip">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * ShipDialog — 卖家发货登记业务组件（FR-C10 / C19 / 3.15 + 3.25）
 *
 * 「开始发货」必须同时登记物流服务商 + 运单号（D-C08），服务商清单来自 3.25。
 * 提交调用 3.15（携带 body），删除了 v3.0 的发货前容量校验，改为凭预占放行。
 * 成功后 emit('shipped') 由父页面刷新订单列表。
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogisticsProviders, shipOrder } from '../../api/seller'

const props = defineProps({
  show:  { type: Boolean, default: false },
  order: { type: Object,  default: null }
})
const emit = defineEmits(['update:show', 'shipped'])

const formRef     = ref(null)
const providers   = ref([])
const loadingProviders = ref(false)
const submitting  = ref(false)
const form = reactive({ logisticsProviderId: null, waybillNo: '' })

const rules = {
  logisticsProviderId: [{ required: true, message: '请选择物流服务商', trigger: 'change' }],
  waybillNo: [{ required: true, message: '运单号不能为空', trigger: 'blur' }]
}

const loadProviders = async () => {
  loadingProviders.value = true
  try {
    const res = await getLogisticsProviders()
    providers.value = res?.data || []
  } catch { providers.value = [] }
  loadingProviders.value = false
}

const reset = () => {
  form.logisticsProviderId = null
  form.waybillNo = ''
  formRef.value?.clearValidate()
}
watch(() => props.show, (v) => { if (v) reset() })

const handleShip = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await shipOrder(props.order.id, {
        logisticsProviderId: form.logisticsProviderId,
        waybillNo: form.waybillNo.trim()
      })
      if (res?.code === 200) {
        ElMessage.success('已登记发货')
        emit('shipped')
        emit('update:show', false)
      } else {
        ElMessage.error(res?.msg || '发货失败')
      }
    } finally { submitting.value = false }
  })
}
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
