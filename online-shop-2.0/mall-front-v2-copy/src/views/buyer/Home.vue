<template>
  <div style="max-width:1200px;margin:30px auto;padding:0 20px">
    <h2>商品列表</h2>
    <!-- 搜索 + 分类筛选区域 -->
    <div style="margin-bottom:16px;display:flex;gap:10px;align-items:center;flex-wrap:wrap">
      <el-input v-model="keyword" placeholder="输入商品名称搜索" style="width:300px" @keyup.enter="loadData"/>
      <el-button type="primary" @click="loadData">搜索</el-button>

      <!-- 二级分类筛选下拉，来自 /api/buyer/category/tree -->
      <el-select v-model="query.categoryId" placeholder="选择二级分类" clearable @change="loadData" style="width:200px">
        <el-option
          v-for="item in flatCategory"
          :key="item.id"
          :label="item.categoryName"
          :value="item.id"
        />
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
import { getBuyerProductList, searchProduct, getBuyerCategoryTree } from '../../api/buyer'

const query = ref({
  page: 1,
  size: 10,
  categoryId: null
})
const pageData = ref({ list: [], total: 0 })
const keyword = ref('')

// 分类树数据
const categoryTree = ref([])
const flatCategory = ref([])

// 加载分类树：调用 /api/buyer/category/tree
const loadCategory = async () => {
  const res = await getBuyerCategoryTree()
  categoryTree.value = res.data
  // 只取出所有二级分类，平铺作为下拉选项
  flatCategory.value = categoryTree.value.flatMap(p => p.children || [])
}

const loadData = async () => {
  // ============ 正式接口版本（联调打开） ============
  const k = keyword.value.trim()
  if (k) {
    // 搜索模式 FR-A06
    const res = await searchProduct({ ...query.value, name: k })
    pageData.value = res.data
  } else {
    // 浏览模式 FR-A05：只传 categoryId、page、size，不传sort
    const res = await getBuyerProductList(query.value)
    pageData.value = res.data
  }
  // --------------------------------------------------
}

const resetSearch = () => {
  keyword.value = ''
  query.value.categoryId = null
  query.value.page = 1
  loadData()
}

onMounted(() => {
  loadCategory() // 页面一进来先拉分类树
  loadData()
})
</script>
