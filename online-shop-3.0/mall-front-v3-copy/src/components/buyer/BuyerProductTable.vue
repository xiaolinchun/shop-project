<template>
  <div>
    <!-- ① 搜索 / 筛选工具栏 -->
    <div class="product-table__toolbar">
      <el-input
        v-model="query.keyword" placeholder="输入商品名称搜索" clearable
        style="width:260px" @keyup.enter="onSearch" @clear="onSearch"
      />
      <el-select v-model="query.categoryId" placeholder="选择分类" clearable style="width:160px" @change="onSearch">
        <el-option v-for="c in flatCategory" :key="c.id" :label="c.categoryName" :value="c.id" />
      </el-select>
      <el-select v-model="query.sort" style="width:140px" @change="onSearch">
        <el-option label="最新发布" value="TIME_DESC" />
        <el-option label="最早发布" value="TIME_ASC" />
        <el-option label="按分类" value="CATEGORY" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <!-- ② 业务表格 -->
    <BizTable
      :data="list" :total="total"
      v-model:page="query.page" v-model:page-size="query.size"
      :loading="loading"
    >
      <el-table-column label="主图" width="80">
        <template #default="{ row }">
          <el-image
            v-if="row.mainImageUrl"
            :src="row.mainImageUrl"
            style="width:50px;height:50px" fit="cover"
            :preview-src-list="[row.mainImageUrl]"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="商品名称" prop="name" min-width="160" />
      <el-table-column label="当前价" width="130">
        <template #default="{ row }">
          <span v-if="row.salePrice && row.currentPrice < row.price" style="color:#e6a23c;font-weight:bold">
            ¥{{ row.currentPrice }}
          </span>
          <span v-else>¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="原价" width="100">
        <template #default="{ row }">
          <span v-if="row.salePrice && row.currentPrice < row.price" style="text-decoration:line-through;color:#999">
            ¥{{ row.price }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="categoryName" width="120" />
      <el-table-column label="发布时间" prop="publishedAt" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('detail', row.id)">查看详情</el-button>
          <el-button size="small" type="primary" @click="$emit('add-cart', row)">加购</el-button>
        </template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * BuyerProductTable — 买家商品列表业务组件
 *
 * 封装搜索、分类筛选、排序、分页，页面直接 <BuyerProductTable /> 引入。
 * 仅返回在售商品（OFFLINE 不出现）。
 */
import { ref, reactive, onMounted } from 'vue'
import { getBuyerProductList, getBuyerCategoryTree } from '../../api/buyer'
import BizTable from '../BizTable.vue'

const list       = ref([])
const total      = ref(0)
const loading    = ref(false)
const flatCategory = ref([])

const query = reactive({
  page: 1, size: 10,
  keyword: '', categoryId: null, sort: 'TIME_DESC'
})

const emit = defineEmits(['detail', 'add-cart'])

// ─── 方法 ───
const loadCategories = async () => {
  try {
    const res = await getBuyerCategoryTree()
    const tree = res?.data || []
    flatCategory.value = tree.flatMap(p => p.children || [])
  } catch { /* 兜底空列表 */ }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size, sort: query.sort }
    if (query.keyword)    params.keyword   = query.keyword.trim()
    if (query.categoryId) params.categoryId = query.categoryId
    const res = await getBuyerProductList(params)
    if (res?.code === 200 && res.data) {
      list.value  = res.data.list || []
      total.value = res.data.total  || 0
    } else {
      list.value = []; total.value = 0
    }
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const onSearch = () => { query.page = 1; loadData() }
const onReset  = () => {
  query.keyword = ''; query.categoryId = null; query.sort = 'TIME_DESC'
  onSearch()
}

onMounted(() => { loadCategories(); loadData() })
defineExpose({ refresh: loadData })
</script>

<style scoped>
.product-table__toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
