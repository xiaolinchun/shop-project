<template>
  <div>
    <BizTable
      :data="list" :total="total"
      v-model:page="page" v-model:page-size="size"
      :loading="loading"
    >
      <el-table-column label="用户名" prop="username" />
      <el-table-column label="手机号" prop="phone" />
      <el-table-column label="默认交易地点">
        <template #default="{ row }">
          {{ row.defaultTradeLocation || '未设置' }}
        </template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createdAt" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('view-history', row.username)">
            查看购买历史
          </el-button>
        </template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * CustomerTable — 客户列表业务组件
 *
 * 封装客户数据的加载与分页，页面引入 <CustomerTable /> 即可。
 * 行级「查看购买历史」操作通过 emit 交给父页面处理。
 */
import { ref, onMounted } from 'vue'
import { getCustomerList } from '../../api/seller'
import BizTable from '../BizTable.vue'

const list    = ref([])
const total   = ref(0)
const loading = ref(false)
const page    = ref(1)
const size    = ref(10)

const emit = defineEmits(['view-history'])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCustomerList({ page: page.value, size: size.value })
    if (res?.code === 200 && res.data) {
      list.value  = res.data.list || []
      total.value = res.data.total || 0
    } else {
      list.value = []; total.value = 0
    }
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

onMounted(() => loadData())
defineExpose({ refresh: loadData })
</script>
