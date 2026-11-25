<template>
  <el-dialog
    :model-value="show" title="申请售后" width="560px"
    @update:model-value="v => emit('update:show', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="选择商品" prop="itemId">
        <el-select v-model="form.itemId" placeholder="选择要申请的订单商品" style="width:100%">
          <el-option
            v-for="it in items" :key="it.id" :value="it.id"
            :label="`${it.productName} × ${it.quantity}`"
            :disabled="it.afterSalesOngoing"
          />
        </el-select>
        <div class="muted">按订单明细单件发起；同一明细同时仅 1 个进行中售后单（驳回后可重发）。</div>
      </el-form-item>
      <el-form-item label="申请标题" prop="title">
        <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="如：收到商品有划痕" />
      </el-form-item>
      <el-form-item label="问题描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="详细描述商品问题" />
      </el-form-item>
      <el-form-item label="凭证图片">
        <ImageUploader v-model="form.images" :max="3" :max-size="5" token-type="buyer" tip="最多 3 张，单张 ≤ 5MB（jpg / png）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:show', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * AfterSalesForm — 买家发起售后申请业务组件（FR-C12 / 2.27）
 *
 * 售后窗口自「开始发货」起（SHIPPING / COMPLETED）。申请单：标题必填、描述必填、
 * 图片 0~3 张。组件内部完成图片上传 + 明细选择 + 2.27 提交，成功后 emit('submitted')。
 * 售后为平行单据：不改写订单状态、不触碰库存与支付。
 */
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createAfterSales } from '../../api/buyer'
import ImageUploader from '../common/ImageUploader.vue'

const props = defineProps({
  show:  { type: Boolean, default: false },
  order: { type: Object,  default: null }   // 含 items[]
})
const emit = defineEmits(['update:show', 'submitted'])

const formRef    = ref(null)
const submitting = ref(false)
const form = reactive({ itemId: null, title: '', description: '', images: [] })

const items = computed(() => props.order?.items || [])

const rules = {
  itemId:      [{ required: true, message: '请选择要申请的商品', trigger: 'change' }],
  title:       [{ required: true, message: '申请标题不能为空', trigger: 'blur' }],
  description: [{ required: true, message: '问题描述不能为空', trigger: 'blur' }]
}

const reset = () => {
  form.itemId = items.value.length === 1 ? items.value[0].id : null
  form.title = ''
  form.description = ''
  form.images = []
  formRef.value?.clearValidate()
}

watch(() => props.show, (v) => { if (v) reset() })

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await createAfterSales(form.itemId, {
        title: form.title,
        description: form.description,
        images: form.images
      })
      if (res?.code === 200) {
        ElMessage.success('售后申请已提交')
        emit('submitted')
        emit('update:show', false)
      } else {
        ElMessage.error(res?.msg || '提交失败')
      }
    } finally { submitting.value = false }
  })
}
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
