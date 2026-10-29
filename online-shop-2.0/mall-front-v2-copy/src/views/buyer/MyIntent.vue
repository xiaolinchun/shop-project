<template>
  <div style="max-width:1000px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>我的历史意向记录</h2>
    <el-table :data="pageData.list" border style="margin-top:16px">
      <el-table-column label="商品名称" prop="productName"></el-table-column>
      <el-table-column label="下单时间" prop="createdAt"></el-table-column>
      <el-table-column label="交易结果">
        <template #default="{row}">
          <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
            {{ row.result }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果时间" prop="resultAt"></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="pageData.total" @change="loadData" style="margin-top:16px"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBuyerIntentHistory } from '../../api/buyer'
const query = ref({ page:1, size:10 })
const pageData = ref({ list:[], total:0 })
const loadData = async () => {
  const res = await getBuyerIntentHistory(query.value)
  pageData.value = res.data
}
onMounted(()=> loadData())
</script>
