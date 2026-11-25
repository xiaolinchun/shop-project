<template>
  <div class="image-uploader">
    <el-upload
      :file-list="fileList"
      list-type="picture-card"
      :accept="accept"
      :limit="max"
      :disabled="disabled"
      :on-preview="onPreview"
      :on-remove="onRemove"
      :http-request="customUpload"
      :before-upload="beforeUpload"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <div v-if="tip" class="image-uploader__tip">{{ tip }}</div>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" width="600px" append-to-body>
      <img v-if="previewUrl" :src="previewUrl" style="width:100%" alt="预览" />
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * ImageUploader — 通用图片上传基础组件（Element Plus el-upload 二次封装）
 *
 * 复用 v4.0 的商品图片上传接口（3.3 /api/seller/product/image/upload），
 * 售后申请图片、评价图片均「先上传得到 URL，再随业务请求提交 URL 数组」。
 *
 * 通过 tokenType 决定携带哪个身份 token：
 *   - 卖家场景 tokenType="seller"
 *   - 买家场景 tokenType="buyer"（复用同一上传接口，携带买家 token）
 *
 * v-model 绑定的是「图片 URL 数组」，父组件直接把它放进业务请求体。
 *
 * Props
 *   modelValue : string[]  已上传图片 URL 数组（v-model）
 *   max        : number    最大张数（默认 3，对应售后 / 评价 0~3 张口径）
 *   maxSize    : number    单张大小上限 MB（默认 5）
 *   tokenType  : string    'buyer' | 'seller'（决定上传请求携带的 token）
 *   tip        : string    底部提示文案
 *   disabled   : boolean   是否禁用
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../api/request'

const props = defineProps({
  modelValue: { type: Array,   default: () => [] },
  max:        { type: Number,  default: 3 },
  maxSize:    { type: Number,  default: 5 },
  tokenType:  { type: String,  default: 'seller' },
  accept:     { type: String,  default: '.jpg,.jpeg,.png' },
  tip:        { type: String,  default: '' },
  disabled:   { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const previewVisible = ref(false)
const previewUrl = ref('')

// el-upload 的 file-list 由 URL 数组派生
const fileList = computed(() =>
  props.modelValue.map((url, i) => ({ name: `image-${i}`, url }))
)

const beforeUpload = (file) => {
  const okType = /image\/(jpeg|png)/.test(file.type)
  if (!okType) { ElMessage.error('仅支持 jpg / png 格式'); return false }
  if (file.size / 1024 / 1024 > props.maxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`); return false
  }
  return true
}

const customUpload = async (opt) => {
  const fd = new FormData()
  fd.append('file', opt.file)
  try {
    const res = await request({
      url: '/seller/product/image/upload',
      method: 'POST',
      data: fd,
      headers: { 'Content-Type': 'multipart/form-data' },
      tokenType: props.tokenType
    })
    if (res?.code === 200 && res.data) {
      emit('update:modelValue', [...props.modelValue, res.data])
      opt.onSuccess(res)
    } else {
      ElMessage.error(res?.msg || '图片上传失败')
      opt.onError(new Error(res?.msg || '上传失败'))
    }
  } catch (err) {
    opt.onError(err)
  }
}

const onRemove = (file) => {
  const url = file.url || file.response?.data
  emit('update:modelValue', props.modelValue.filter(u => u !== url))
}

const onPreview = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}
</script>

<style scoped>
.image-uploader__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
