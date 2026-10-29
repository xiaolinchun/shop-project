<<<<<<< HEAD
<template>
  <el-container style="max-width:1100px;margin:30px auto;padding:0 20px">
    <el-page-header @back="$router.push('/seller/home')" content="意向队列"></el-page-header>
    <el-button @click="loadList">刷新列表</el-button>
    <el-table :data="tableData" border style="margin-top:15px">
      <el-table-column prop="id" label="意向ID"></el-table-column>
      <el-table-column prop="buyerName" label="买家姓名"></el-table-column>
      <el-table-column prop="phone" label="手机号"></el-table-column>
      <el-table-column label="状态">
        <template #default="{row}">{{ INTENT_STATUS[row.status] }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间"></el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="mark(row.id,'SUCCESS')">交易成功</el-button>
          <el-button size="small" type="warning" @click="mark(row.id,'FAILURE')">交易作废</el-button>
          <el-button size="small" type="info" @click="mark(row.id,'RE_QUEUE')">重新排队</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getIntentList, markTradeResult } from '../../api/mall'
import { INTENT_STATUS } from '../../constants'
import { ElMessage } from 'element-plus'
const route = useRoute()
const router = useRouter()
const productId = ref(Number(route.params.productId))
const tableData = ref([])

async function loadList() {
  try {
    tableData.value = await getIntentList(productId.value)
  } catch (e) {
    ElMessage.error(e?.msg || "加载意向队列失败")
  }
}

onMounted(() => {
  if (!localStorage.getItem('sellerUser')) {
    router.replace('/seller/login')
    return
  }
  if (isNaN(productId.value)) {
    ElMessage.error("商品ID参数异常")
    router.push('/seller/home')
    return
  }
  loadList()
})

async function mark(intentId, operate) {
  try {
    await markTradeResult({ intentId, operate })
    ElMessage.success('操作完成')
    loadList()
  } catch (e) {
    ElMessage.error(e?.msg || "操作失败")
  }
}
</script>
=======
<template>
  <el-container style="max-width:1100px;margin:30px auto;padding:0 20px">
    <el-page-header @back="$router.push('/seller/home')" content="意向队列"></el-page-header>
    <el-button @click="loadList">刷新列表</el-button>
    <el-table :data="tableData" border style="margin-top:15px">
      <el-table-column prop="id" label="意向ID"></el-table-column>
      <el-table-column prop="buyerName" label="买家姓名"></el-table-column>
      <el-table-column prop="phone" label="手机号"></el-table-column>
      <el-table-column label="状态">
        <template #default="{row}">{{ INTENT_STATUS[row.status] }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间"></el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="mark(row.id,'SUCCESS')">交易成功</el-button>
          <el-button size="small" type="warning" @click="mark(row.id,'FAILURE')">交易作废</el-button>
          <el-button size="small" type="info" @click="mark(row.id,'RE_QUEUE')">重新排队</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getIntentList, markTradeResult } from '../../api/mall'
import { INTENT_STATUS } from '../../constants'
import { ElMessage } from 'element-plus'
const route = useRoute()
const router = useRouter()
const productId = ref(Number(route.params.productId))
const tableData = ref([])

async function loadList() {
  try {
    tableData.value = await getIntentList(productId.value)
  } catch (e) {
    ElMessage.error(e?.msg || "加载意向队列失败")
  }
}

onMounted(() => {
  if (!localStorage.getItem('sellerUser')) {
    router.replace('/seller/login')
    return
  }
  if (isNaN(productId.value)) {
    ElMessage.error("商品ID参数异常")
    router.push('/seller/home')
    return
  }
  loadList()
})

async function mark(intentId, operate) {
  try {
    await markTradeResult({ intentId, operate })
    ElMessage.success('操作完成')
    loadList()
  } catch (e) {
    ElMessage.error(e?.msg || "操作失败")
  }
}
</script>
>>>>>>> 541088904427b67d3edbb35c4de84031bb23e55e
