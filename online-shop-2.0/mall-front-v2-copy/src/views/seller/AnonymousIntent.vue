<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>存量匿名意向归档（只读）</h2>
    <el-table :data="pageData.list" border>
      <el-table-column label="意向ID" prop="id"></el-table-column>
      <el-table-column label="商品ID" prop="productId"></el-table-column>
      <el-table-column label="商品快照" prop="productName"></el-table-column>
      <el-table-column label="旧姓名" prop="buyerName"></el-table-column>
      <el-table-column label="旧电话" prop="phone"></el-table-column>
      <el-table-column label="状态" prop="status"></el-table-column>
      <el-table-column label="创建时间" prop="createdAt"></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="pageData.total" @change="load"></el-pagination>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAnonymousIntentList } from '../../api/seller'
const query = ref({ page:1, size:10 })
const pageData = ref({ list:[], total:0 })
const load = async () => {
  const res = await getAnonymousIntentList(query.value)
  pageData.value = res.data
}
onMounted(()=> load())
</script>
