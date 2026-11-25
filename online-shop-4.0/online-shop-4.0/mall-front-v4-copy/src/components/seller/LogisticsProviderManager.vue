<template>
  <div>
    <div class="lp-toolbar">
      <el-input
        v-model="newName" placeholder="物流服务商名称（如：顺丰速运）"
        style="width:260px" maxlength="30" @keyup.enter="handleAdd"
      />
      <el-button type="primary" :loading="adding" :disabled="!newName.trim()" @click="handleAdd">
        新增服务商
      </el-button>
      <span class="muted">清单由卖家维护，数量不限；已被订单使用的服务商删除后，历史订单仍保留名称快照。</span>
    </div>

    <BizTable :data="list" :total="list.length" :page="1" :page-size="100" :loading="loading">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="服务商名称" prop="name" min-width="200" />
      <el-table-column label="创建时间" prop="createdAt" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * LogisticsProviderManager — 卖家物流服务商清单业务组件（FR-C10 / 3.25–3.27）
 *
 * 内聚服务商列表的查询 / 新增 / 删除，发货登记（ShipDialog）从这份清单选择服务商。
 * 对外 emit('change', list)，便于父页面同步刷新发货弹窗中的下拉。
 */
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogisticsProviders, addLogisticsProvider, deleteLogisticsProvider } from '../../api/seller'
import BizTable from '../BizTable.vue'

const list    = ref([])
const loading = ref(false)
const adding  = ref(false)
const newName = ref('')

const emit = defineEmits(['change'])

const load = async () => {
  loading.value = true
  try {
    const res = await getLogisticsProviders()
    list.value = res?.data || []
    emit('change', list.value)
  } catch { list.value = [] }
  loading.value = false
}

const handleAdd = async () => {
  const name = newName.value.trim()
  if (!name) return
  adding.value = true
  try {
    const res = await addLogisticsProvider({ name })
    if (res?.code === 200) {
      ElMessage.success('已新增物流服务商')
      newName.value = ''
      load()
    } else {
      ElMessage.error(res?.msg || '新增失败')
    }
  } finally { adding.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除物流服务商「${row.name}」吗？`, '删除确认', { type: 'warning' })
    const res = await deleteLogisticsProvider(row.id)
    if (res?.code === 200) { ElMessage.success('已删除'); load() }
    else ElMessage.error(res?.msg || '删除失败')
  } catch { /* 用户取消 */ }
}

load()
defineExpose({ refresh: load })
</script>

<style scoped>
.lp-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.muted { color: #909399; font-size: 12px; }
</style>
