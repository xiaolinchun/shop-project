<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <el-button @click="$router.push('/seller/product-list')">返回商品列表</el-button>
    <h3>商品 '{{ productName }}' 的意向队列&nbsp;&nbsp;&nbsp;&nbsp;商品Id:{{productId}}</h3>
    <el-table :data="intentList" border style="margin-top:12px">
      <el-table-column label="意向ID" prop="id" width="80"></el-table-column>
      <el-table-column label="买家用户名" prop="buyerUsername"></el-table-column>
      <el-table-column label="意向状态">
        <template #default="{row}">
          <el-tag>{{row.status}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建排队时间" prop="createdAt"></el-table-column>
      <el-table-column label="更新时间" prop="updatedAt"></el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" v-if="row.status==='QUEUED' && isFirst(row.id)" @click="markTrade(row.id,'START')">START进入交易</el-button>
          <el-button size="small" v-if="row.status==='TRADING'" @click="markTrade(row.id,'SUCCESS')">SUCCESS交易成功</el-button>
          <el-button size="small" v-if="row.status==='TRADING'" @click="markTrade(row.id,'FAILURE')">FAILURE作废</el-button>
          <el-button size="small" v-if="row.status==='TRADING'" @click="markTrade(row.id,'RE_QUEUE')">RE_QUEUE重新排队(队尾)</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getIntentList, tradeMark } from '../../api/seller'
const route = useRoute()
const router = useRouter()
const productId = ref(Number(route.params.productId))
// 优先从路由query拿到商品名称
const productName = ref(route.query.productName || '未知商品')
const intentList = ref([])

const loadQueue = async () => {
  const res = await getIntentList(productId.value)
  intentList.value = res.data
}
const isFirst = (intentId) => {
  const queued = intentList.value.filter(i=>i.status === 'QUEUED')
  if(queued.length ===0) return false
  return queued[0].id === intentId
}
const markTrade = async (intentId, operate) => {
  await tradeMark({ intentId, operate })
  ElMessage.success(`执行${operate}成功`)
  loadQueue()
}
onMounted(()=> loadQueue())
</script>
