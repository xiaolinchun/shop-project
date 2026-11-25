<template>
  <el-dialog
    :model-value="show" title="发表评价" width="560px"
    @update:model-value="v => emit('update:show', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="选择商品" prop="itemId">
        <el-select v-model="form.itemId" placeholder="选择要评价的订单商品" style="width:100%">
          <el-option
            v-for="it in items" :key="it.id" :value="it.id"
            :label="`${it.productName} × ${it.quantity}`"
            :disabled="it.reviewed || it.refundClosed"
          >
            <span>{{ it.productName }} × {{ it.quantity }}</span>
            <span v-if="it.reviewed" class="muted" style="float:right">已评价</span>
            <span v-else-if="it.refundClosed" class="muted" style="float:right">已退款</span>
          </el-option>
        </el-select>
        <div class="muted">仅「交易完成」订单可评价，每明细一次，提交后不可修改 / 删除。</div>
      </el-form-item>
      <el-form-item label="评分" prop="rating">
        <el-rate v-model="form.rating" :max="5" show-text
          :texts="['很差', '较差', '一般', '满意', '非常满意']" />
      </el-form-item>
      <el-form-item label="文字评价" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="选填，最多 200 字" />
      </el-form-item>
      <el-form-item label="图片">
        <ImageUploader v-model="form.images" :max="3" :max-size="5" token-type="buyer" tip="最多 3 张，单张 ≤ 5MB（jpg / png）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:show', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * ReviewForm — 买家提交评价业务组件（FR-C21 / 2.29）
 *
 * 仅 COMPLETED 订单明细可评价，每明细一次；评分 1~5 必填、文字 ≤200 选填、图片 0~3。
 * 已评价 / 已退款闭环的明细不可选（禁用），后端二次校验。成功后 emit('submitted')。
 */
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createReview } from '../../api/buyer'
import ImageUploader from '../common/ImageUploader.vue'

const props = defineProps({
  show:  { type: Boolean, default: false },
  order: { type: Object,  default: null }
})
const emit = defineEmits(['update:show', 'submitted'])

const formRef    = ref(null)
const submitting = ref(false)
const form = reactive({ itemId: null, rating: 5, content: '', images: [] })

// 可评价明细：未评价 && 未退款闭环
const items = computed(() => props.order?.items || [])
const eligible = computed(() => items.value.filter(it => !it.reviewed && !it.refundClosed))

const rules = {
  itemId: [{ required: true, message: '请选择要评价的商品', trigger: 'change' }],
  rating: [{ required: true, type: 'number', min: 1, message: '请打分（1~5 星）', trigger: 'change' }]
}

const reset = () => {
  form.itemId = eligible.value.length === 1 ? eligible.value[0].id : null
  form.rating = 5
  form.content = ''
  form.images = []
  formRef.value?.clearValidate()
}

watch(() => props.show, (v) => { if (v) reset() })

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await createReview(form.itemId, {
        rating: form.rating,
        content: form.content,
        images: form.images
      })
      if (res?.code === 200) {
        ElMessage.success('评价已提交')
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
