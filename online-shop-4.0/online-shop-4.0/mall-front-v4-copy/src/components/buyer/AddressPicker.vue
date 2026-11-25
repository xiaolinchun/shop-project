<template>
  <div class="addr-picker">
    <div v-loading="loading">
      <el-radio-group
        v-if="list.length" :model-value="modelValue" class="addr-picker__group"
        @update:model-value="v => emit('update:modelValue', v)"
      >
        <el-radio
          v-for="a in list" :key="a.id" :value="a.id"
          class="addr-picker__item" border
        >
          <div class="addr-picker__line">
            <strong>{{ a.name }}</strong>
            <span class="muted">{{ a.phone }}</span>
            <el-tag v-if="a.isDefault" type="success" size="small">默认</el-tag>
          </div>
          <div class="addr-picker__line muted">
            {{ [a.province, a.city, a.district].filter(Boolean).join('') }}{{ a.detail }}
          </div>
        </el-radio>
      </el-radio-group>

      <el-empty v-else :image-size="60" description="还没有收货地址，请先添加" />
    </div>

    <div style="margin-top:10px">
      <el-button size="small" :disabled="list.length >= 20" @click="openAdd">＋ 使用新地址</el-button>
    </div>

    <!-- 快速新增地址（轻量表单，保存后自动选中） -->
    <el-dialog v-model="addDialog" title="新增收货地址" width="480px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="电话" prop="phone"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
        <el-form-item label="所在地区" required>
          <div style="display:flex;gap:6px;width:100%">
            <el-form-item prop="province" style="flex:1;margin-bottom:0"><el-input v-model="form.province" placeholder="省" /></el-form-item>
            <el-form-item prop="city" style="flex:1;margin-bottom:0"><el-input v-model="form.city" placeholder="市" /></el-form-item>
            <el-form-item prop="district" style="flex:1;margin-bottom:0"><el-input v-model="form.district" placeholder="区" /></el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail"><el-input v-model="form.detail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdd">保存并选用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * AddressPicker — 下单收货地址选择器（FR-C09）
 *
 * 载入买家地址簿、默认地址自动选中（modelValue 初值由父页面或本组件设为默认地址 id），
 * 支持切换与「快速新增」。新增/加载后通过 update:modelValue 把选中的 addressId 回传父页面，
 * 父页面下单时携带 addressId，地址随即写入订单快照（下单后不可改，CA-C8）。
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAddressList, addAddress } from '../../api/buyer'

const props = defineProps({
  modelValue: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue', 'loaded'])

const list    = ref([])
const loading = ref(false)
const addDialog = ref(false)
const saving  = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false
})
const rules = {
  name:     [{ required: true, message: '收货人姓名不能为空', trigger: 'blur' }],
  phone:    [{ required: true, message: '手机号不能为空', trigger: 'blur' },
             { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请填写省', trigger: 'blur' }],
  city:     [{ required: true, message: '请填写市', trigger: 'blur' }],
  district: [{ required: true, message: '请填写区', trigger: 'blur' }],
  detail:   [{ required: true, message: '详细地址不能为空', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    list.value = res?.data || []
    // 未选中时自动带入默认地址（无默认则选第一个）
    if (!props.modelValue && list.value.length) {
      const def = list.value.find(a => a.isDefault) || list.value[0]
      emit('update:modelValue', def.id)
    }
    emit('loaded', list.value)
  } catch { list.value = [] }
  loading.value = false
}

const openAdd = () => {
  Object.assign(form, { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })
  addDialog.value = true
}

const handleAdd = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res = await addAddress({ ...form })
      if (res?.code === 200) {
        ElMessage.success('地址已新增')
        addDialog.value = false
        await load()
        if (res.data?.id) emit('update:modelValue', res.data.id)
      } else {
        ElMessage.error(res?.msg || '新增失败')
      }
    } finally { saving.value = false }
  })
}

watch(() => props.modelValue, () => {}, { immediate: false })
load()
defineExpose({ refresh: load })
</script>

<style scoped>
.addr-picker__group { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.addr-picker__item { width: 100%; margin-right: 0 !important; height: auto; padding: 8px 12px; }
.addr-picker__item :deep(.el-radio__label) { display: block; width: 100%; text-align: left; }
.addr-picker__line { line-height: 1.6; }
.muted { color: #909399; font-size: 13px; }
</style>
