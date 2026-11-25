<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>已下架商品</h2>
    <p style="color:#999;font-size:13px;margin-bottom:12px">
      以下为当前所有已下架商品。已下架非终态，补货后可在「我的商品」中手动上架。
    </p>

    <BizTable
      :data="list" :total="list.length"
      :page="1" :page-size="100"
      :loading="loading"
    >
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="商品名称" prop="name" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="80" />
      <el-table-column label="下架原因" width="140">
        <template #default="{ row }">
          <el-tag type="info">
            {{ row.offReason === 'STOCK_OUT' ? '库存归零' : '手动下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下架时间" prop="closedAt" width="170" />
    </BizTable>
  </div>
</template>

<script setup>
/**
 * HistoryProduct — 卖家「已下架商品」页面
 *
 * v3.0 语义变更：「历史商品」即所有 OFFLINE 商品，
 * 补货上架后即从本列表消失。使用 BizTable 基础组件。
 */
import { ref, onMounted } from 'vue'
import { getSellerHistoryProduct } from '../../api/seller'
import BizTable from '../../components/BizTable.vue'

const list    = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSellerHistoryProduct()
    list.value = res.data || []
  } catch { list.value = [] }
  loading.value = false
}

onMounted(() => loadData())
</script>
