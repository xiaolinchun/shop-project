<template>
  <div>
    <!-- ① 搜索 / 筛选工具栏 -->
    <div class="product-table__toolbar">
      <el-input
        v-model="query.keyword" placeholder="搜索商品名称" clearable
        style="width:220px" @keyup.enter="onSearch" @clear="onSearch"
      />
      <el-select v-model="query.categoryId" placeholder="选择分类" clearable style="width:160px" @change="onSearch">
        <el-option v-for="c in flatCategory" :key="c.id" :label="c.categoryName" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="商品状态" clearable style="width:140px" @change="onSearch">
        <el-option label="在售" value="ONLINE" />
        <el-option label="已下架" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
      <slot name="toolbar-extra" />
    </div>

    <!-- ② 业务表格（BizTable 统一管理分页 + 加载态） -->
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
      <el-table-column label="商品名称" prop="name" min-width="140" />
      <el-table-column label="发布价" prop="price" width="100" />
      <el-table-column label="适用价" width="100">
        <template #default="{ row }">
          <span :style="row.currentPrice < row.price ? 'color:#e6a23c;font-weight:bold' : ''">
            {{ row.currentPrice ?? row.price }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'">
            {{ row.status === 'ONLINE' ? '在售' : '已下架' }}
          </el-tag>
          <el-tag v-if="row.pendingAlert" type="danger" size="small" style="margin-left:4px">
            库存不足·{{ row.pendingQuantity }}单待发货
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="categoryName" width="110" />
      <el-table-column label="发布时间" prop="publishedAt" width="170" />
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$emit('detail', row.id)">查看</el-button>
          <el-button size="small" @click="$emit('order-manage', row.id)">订单管理</el-button>
          <el-button size="small" @click="$emit('edit-stock', row)">改库存</el-button>
          <el-button size="small" @click="$emit('edit-category', row)">改分类</el-button>
          <el-button size="small" type="warning" @click="$emit('set-sale', row)">活动价</el-button>
          <el-button
            v-if="row.status === 'ONLINE'" size="small" type="danger"
            @click="$emit('offline', row.id)"
          >下架</el-button>
          <el-button
            v-if="row.status === 'OFFLINE'" size="small" type="success"
            @click="$emit('online', row.id)"
          >上架</el-button>
        </template>
      </el-table-column>
    </BizTable>
  </div>
</template>

<script setup>
/**
 * SellerProductTable — 卖家商品列表业务组件
 *
 * 内部封装了搜索表单、状态 / 分类筛选、BizTable 表格与分页，
 * 页面只需 <SellerProductTable /> 即可获得完整的商品管理列表。
 *
 * 所有行级操作通过 emit 暴露给父页面，由父页面决定如何响应（弹窗 / 跳转 …）。
 */
import { ref, reactive, onMounted } from 'vue'
import { getSellerProductList, getSellerCategoryTree } from '../../api/seller'
import BizTable from '../BizTable.vue'

// ─── 数据 ───
const list       = ref([])
const total      = ref(0)
const loading    = ref(false)
const categories = ref([])
const flatCategory = ref([])

// ─── 查询参数 ───
const query = reactive({
  page: 1, size: 10,
  keyword: '', categoryId: null, status: null
})

// ─── 事件 ───
const emit = defineEmits([
  'detail', 'order-manage', 'edit-stock', 'edit-category',
  'set-sale', 'offline', 'online'
])

// ─── 方法 ───
const loadCategories = async () => {
  try {
    const res = await getSellerCategoryTree()
    categories.value = res.data || []
    flatCategory.value = categories.value.flatMap(p => p.children || [])
  } catch { /* 兜底空列表 */ }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword)    params.keyword   = query.keyword.trim()
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.status)     params.status     = query.status
    const res = await getSellerProductList(params)
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
  query.keyword = ''; query.categoryId = null; query.status = null
  onSearch()
}

// ─── 初始化 ───
onMounted(() => { loadCategories(); loadData() })

// 暴露刷新方法，供父页面在弹窗操作后调用
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
