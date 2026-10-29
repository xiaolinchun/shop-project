<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>全部客户列表</h2>
    <el-table :data="pageData.list" border>
      <el-table-column label="用户名" prop="username"></el-table-column>
      <el-table-column label="手机号" prop="phone"></el-table-column>
      <el-table-column label="默认交易地点" prop="defaultTradeLocation">
        <template #default="{row}">
          {{ row.defaultTradeLocation || '未设置' }}
        </template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createdAt"></el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="openHistory(row.username)">查看该客户购买历史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="pageData.total" @change="loadList"></el-pagination>

    <el-dialog v-model="historyDialog.show" title="客户购买历史" width="80%">
      <el-table :data="historyList" border>
        <el-table-column label="商品快照名" prop="productName"></el-table-column>
        <el-table-column label="下单时间" prop="createdAt"></el-table-column>
        <el-table-column label="结果" prop="result"></el-table-column>
        <el-table-column label="结果时间" prop="resultAt"></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCustomerList, getCustomerHistory } from '../../api/seller'
const query = ref({ page:1, size:10 })
const pageData = ref({ list:[], total:0 })
const historyDialog = ref({ show:false })
const historyList = ref([])

const loadList = async () => {
  const res = await getCustomerList(query.value)
  pageData.value = res.data
}
const openHistory = async (username) => {
  historyDialog.value.show = true
  const res = await getCustomerHistory(username, {page:1,size:50})
  historyList.value = res.data.list
}
onMounted(()=> loadList())
</script>
