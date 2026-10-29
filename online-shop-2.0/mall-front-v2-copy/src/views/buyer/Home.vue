<template>
  <div style="max-width:1200px;margin:30px auto;padding:0 20px">
    <h2>商品列表</h2>
    <!-- 搜索 + 分类筛选区域 -->
    <div style="margin-bottom:16px;display:flex;gap:10px;align-items:center;flex-wrap:wrap">
      <el-input v-model="keyword" placeholder="输入商品名称搜索" style="width:300px" @keyup.enter="handleSearch"/>
      <el-button type="primary" @click="handleSearch">搜索</el-button>

      <!-- 二级分类筛选下拉，来自 /api/buyer/category/tree -->
      <el-select v-model="query.categoryId" placeholder="选择二级分类" clearable @change="loadData" style="width:200px">
        <el-option
          v-for="item in flatCategory"
          :key="item.id"
          :label="item.categoryName"
          :value="item.id"
        />
      </el-select>

      <!-- 排序方式下拉 -->
      <el-select v-model="query.sort" placeholder="排序方式" @change="loadData" style="width:160px">
        <el-option label="最新发布" value="TIME_DESC" />
        <el-option label="最早发布" value="TIME_ASC" />
        <el-option label="按分类" value="CATEGORY" />
      </el-select>

      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pageData.list" border>
      <el-table-column label="ID" prop="id"></el-table-column>
      <el-table-column label="商品名称" prop="name"></el-table-column>
      <el-table-column label="价格" prop="price"></el-table-column>
      <el-table-column label="分类名称" prop="categoryName"/>
      <el-table-column label="状态">
        <template #default="{row}">
          <el-tag :type="row.status==='ONLINE'?'success':'info'">{{row.status}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" prop="publishedAt"/>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="$router.push(`/buyer/detail/${row.id}`)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="pageData.total"
      @change="loadData"
      style="margin-top:16px"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBuyerProductList, searchProduct, getBuyerCategoryTree } from '../../api/buyer'

const query = ref({
  page: 1,
  size: 10,
  categoryId: null,
  sort: 'TIME_DESC'
})
const pageData = ref({ list: [], total: 0 })
const keyword = ref('')

// 分类树数据
const categoryTree = ref([])
const flatCategory = ref([])

// 加载分类树：调用 /api/buyer/category/tree
const loadCategory = async () => {
  try {
    const res = await getBuyerCategoryTree()
    categoryTree.value = res.code === 200 && res.data ? res.data : []
    // 只取出所有二级分类，平铺作为下拉选项
    flatCategory.value = categoryTree.value.flatMap(p => p.children || [])
  } catch { /* 拦截器已提示，保持空列表不崩页面 */ }
}

// 点击搜索/回车：先回到第一页再查询（翻页仍走 loadData，不能在里面重置）
const handleSearch = () => {
  query.value.page = 1
  loadData()
}

const loadData = async () => {
  const k = keyword.value.trim()
  try {
    // 搜索模式 FR-A06（keyword）/ 浏览模式 FR-A05（categoryId、page、size、sort）
    const res = k
      ? await searchProduct({ ...query.value, keyword: k })
      : await getBuyerProductList(query.value)
    // 业务码非 200 或 data 为空时兜底，避免 pageData 变 null 导致渲染崩溃
    if (res?.code !== 200 || !res.data) {
      ElMessage.error(res?.msg || '加载商品列表失败')
      pageData.value = { list: [], total: 0 }
      return
    }
    pageData.value = res.data
  } catch {
    // HTTP 层错误（后端未启动/代理失败等），拦截器已弹提示，这里只保数据形状
    pageData.value = { list: [], total: 0 }
  }
}

const resetSearch = () => {
  keyword.value = ''
  query.value.categoryId = null
  query.value.sort = 'TIME_DESC'
  query.value.page = 1
  loadData()
}

onMounted(() => {
  loadCategory() // 页面一进来先拉分类树
  loadData()
})
</script>
