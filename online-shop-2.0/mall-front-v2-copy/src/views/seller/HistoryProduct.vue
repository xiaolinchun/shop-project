<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>历史商品</h2>
    <el-table :data="tableData" border>
      <el-table-column label="商品名称" prop="name"/>
      <el-table-column label="价格" prop="price"/>
      <el-table-column label="库存" prop="stock"/>
      <el-table-column label="状态" prop="status">
        <template #default="{row}">
          <el-tag type="info">{{ row.status === 'OFFLINE' ? '已下架' : row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下架时间" prop="closedAt"/>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSellerHistoryProduct } from '../../api/seller'
const tableData = ref([])

const loadData = async () => {
  const res = await getSellerHistoryProduct()
  tableData.value = res.data
}
onMounted(() => loadData())
</script>
