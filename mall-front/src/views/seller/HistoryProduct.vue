<template>
  <el-container style="max-width:1000px;margin:30px auto;padding:0 20px">
    <el-page-header @back="$router.push('/seller/home')" content="历史已下架商品"></el-page-header>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID"></el-table-column>
      <el-table-column prop="name" label="商品名称"></el-table-column>
      <el-table-column prop="price" label="价格"></el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{row}">{{ PRODUCT_STATUS[row.status] }}</template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="上架时间"></el-table-column>
      <el-table-column prop="closedAt" label="下架时间"></el-table-column>
    </el-table>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHistoryProduct } from '../../api/mall'
import { PRODUCT_STATUS } from '../../constants'

const router = useRouter()
const list = ref([])

async function load() {
  list.value = await getHistoryProduct()
}

onMounted(() => {
  if (!localStorage.getItem('sellerUser')) router.replace('/seller/login')
  load()
})
</script>
