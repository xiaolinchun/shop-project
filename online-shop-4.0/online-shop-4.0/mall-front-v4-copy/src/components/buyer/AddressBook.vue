<template>
  <div>
    <!-- 工具栏：新增地址（数量上限 20，D-C05） -->
    <div class="addr-toolbar">
      <el-button type="primary" :disabled="list.length >= 20" @click="openAdd">
        新增收货地址
      </el-button>
      <span class="muted">共 {{ list.length }} / 20 个地址</span>
    </div>

    <!-- 地址列表（复用 BizTable 表格 + 分页；地址接口返回全量数组） -->
    <BizTable :data="list" :total="list.length" :page="1" :page-size="100" :loading="loading">
      <el-table-column label="收货人" prop="name" width="120" />
      <el-table-column label="联系电话" prop="phone" width="150" />
      <el-table-column label="所在地区" min-width="180">
        <template #default="{ row }">{{ [row.province, row.city, row.district].filter(Boolean).join(' ') }}</template>
      </el-table-column>
      <el-table-column label="详细地址" prop="detail" min-width="200" />
      <el-table-column label="默认" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
          <el-button v-else link type="primary" size="small" @click="handleSetDefault(row)">设为默认</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </BizTable>

    <!-- 新增 / 编辑地址弹窗（业务表单内聚在组件内部） -->
    <el-dialog v-model="dialog.show" :title="dialog.id ? '编辑地址' : '新增地址'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="form.name" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" required>
          <div style="display:flex;gap:8px;width:100%">
            <el-form-item prop="province" style="flex:1;margin-bottom:0">
              <el-input v-model="form.province" placeholder="省" />
            </el-form-item>
            <el-form-item prop="city" style="flex:1;margin-bottom:0">
              <el-input v-model="form.city" placeholder="市" />
            </el-form-item>
            <el-form-item prop="district" style="flex:1;margin-bottom:0">
              <el-input v-model="form.district" placeholder="区" />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道门牌号等详细地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.show = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * AddressBook — 买家地址簿业务组件（FR-C08 / 2.21–2.25）
 *
 * 完整内聚地址的增删改查 + 设为默认逻辑，页面只需 <AddressBook /> 引入。
 * 组件内部组合 BizTable（表格 + 分页）与 el-form 弹窗，对外无 emit，
 * 通过 defineExpose({ refresh }) 允许父页面在需要时手动刷新。
 */
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAddressList, addAddress, updateAddress,
  deleteAddress, setDefaultAddress
} from '../../api/buyer'
import BizTable from '../BizTable.vue'

const list    = ref([])
const loading = ref(false)
const saving  = ref(false)
const formRef = ref(null)

const dialog = reactive({ show: false, id: null })
const emptyForm = () => ({
  name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false
})
const form = reactive(emptyForm())

const rules = {
  name:     [{ required: true, message: '收货人姓名不能为空', trigger: 'blur' }],
  phone:    [{ required: true, message: '手机号不能为空', trigger: 'blur' },
             { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请填写省', trigger: 'blur' }],
  city:     [{ required: true, message: '请填写市', trigger: 'blur' }],
  district: [{ required: true, message: '请填写区', trigger: 'blur' }],
  detail:   [{ required: true, message: '详细地址不能为空', trigger: 'blur' }]
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    list.value = res?.data || []
  } catch { list.value = [] }
  loading.value = false
}

const resetForm = (row) => {
  Object.assign(form, emptyForm(), row ? {
    name: row.name, phone: row.phone, province: row.province,
    city: row.city, district: row.district, detail: row.detail, isDefault: !!row.isDefault
  } : {})
}

const openAdd = () => {
  dialog.id = null
  resetForm(null)
  dialog.show = true
}

const openEdit = (row) => {
  dialog.id = row.id
  resetForm(row)
  dialog.show = true
}

const handleSave = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        name: form.name, phone: form.phone, province: form.province,
        city: form.city, district: form.district, detail: form.detail, isDefault: form.isDefault
      }
      const res = dialog.id
        ? await updateAddress(dialog.id, payload)
        : await addAddress(payload)
      if (res?.code === 200) {
        ElMessage.success(dialog.id ? '地址已更新' : '地址已新增')
        dialog.show = false
        loadList()
      } else {
        ElMessage.error(res?.msg || '保存失败')
      }
    } finally { saving.value = false }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除地址「${row.name} ${row.detail}」吗？历史订单地址快照不受影响。`, '删除地址', { type: 'warning' })
    const res = await deleteAddress(row.id)
    if (res?.code === 200) { ElMessage.success('已删除'); loadList() }
    else ElMessage.error(res?.msg || '删除失败')
  } catch { /* 用户取消 */ }
}

const handleSetDefault = async (row) => {
  const res = await setDefaultAddress(row.id)
  if (res?.code === 200) { ElMessage.success('已设为默认'); loadList() }
  else ElMessage.error(res?.msg || '操作失败')
}

loadList()
defineExpose({ refresh: loadList })
</script>

<style scoped>
.addr-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.muted { color: #909399; font-size: 13px; }
</style>
