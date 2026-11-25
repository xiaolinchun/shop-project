<template>
  <div>
    <BizTable
      :data="list" :total="total"
      v-model:page="page" v-model:page-size="size"
      :loading="loading"
    >
      <el-table-column label="售后单号" prop="afterSalesNo" min-width="150" />
      <el-table-column label="订单号" prop="orderNo" min-width="140" />
      <el-table-column label="商品" prop="productName" min-width="140" />
      <el-table-column label="申请标题" prop="title" min-width="160" />
      <el-table-column label="状态" width="100">
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
      <el-table-column label="申请时间" prop="createdAt" width="165" />
      <el-table-column label="结果时间" prop="resultAt" width="165">
        <template #default="{ row }">{{ row.resultAt || '-' }}</template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * AfterSalesTable — 买家「我的售后单」列表业务组件（FR-C13 / 2.28）
 *
 * 自助查看售后进展（无通知）。封装 2.28 数据加载 + 分页 + 状态 / 结果标签渲染。
 */
import { ref, watch, onMounted } from 'vue'
import { getMyAfterSalesList } from '../../api/buyer'
import { afterSalesStatusLabel, afterSalesStatusType, afterSalesResultLabel, afterSalesResultType } from '../../utils/dict'
import BizTable from '../BizTable.vue'

const list    = ref([])
const total   = ref(0)
const loading = ref(false)
const page    = ref(1)
const size    = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyAfterSalesList({ page: page.value, size: size.value })
    const data = res?.data || res
    list.value  = data?.list || []
    total.value = data?.total || 0
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

watch([page, size], () => loadData())
onMounted(() => loadData())
defineExpose({ refresh: loadData })
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
