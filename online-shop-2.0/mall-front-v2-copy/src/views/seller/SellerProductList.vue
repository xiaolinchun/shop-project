<template>
  <div style="max-width:1200px;margin:30px auto;padding:20px">
    <div style="display:flex;gap:10px">
      <el-button type="primary" @click="$router.push('/seller/publish')">发布新商品</el-button>
    </div>
    <el-select v-model="query.status" placeholder="筛选状态" @change="loadList" style="width:200px;margin:12px 0">
      <el-option label="全部" :value="undefined"></el-option>
      <el-option label="在售 ONLINE" value="ONLINE"></el-option>
      <el-option label="冻结 FROZEN" value="FROZEN"></el-option>
      <el-option label="下架 OFFLINE" value="OFFLINE"></el-option>
    </el-select>

    <el-table :data="pageData.list" border>
      <el-table-column label="ID" prop="id" width="80"></el-table-column>
      <el-table-column label="商品名" prop="name"></el-table-column>
      <el-table-column label="种类" prop="categoryName"></el-table-column>
      <el-table-column label="价格" prop="price"></el-table-column>
      <el-table-column label="库存" prop="stock"></el-table-column>
      <el-table-column label="状态">
        <template #default="{row}">
          <el-tag :type="row.status==='ONLINE'?'success':row.status==='FROZEN'?'warning':'info'">
            {{row.status}}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{row}">
          <el-button size="small" @click="openDetail(row.id)">查看详情</el-button>
          <el-button 
            size="small" 
            @click="$router.push({
              path: `/seller/intent-queue/${row.id}`,
              query: { productName: row.name }
            })"
          >意向队列</el-button>
          <el-button v-if="row.status==='ONLINE'" size="small" type="warning" @click="freeze(row.id)">手动冻结</el-button>
          <el-button v-if="row.status==='FROZEN'" size="small" @click="unfreeze(row.id)">解冻</el-button>
          <el-button v-if="row.status==='ONLINE'" size="small" type="danger" @click="offline(row.id)">下架</el-button>
          <el-button v-if="row.status==='OFFLINE'" size="small" type="primary" @click="online(row.id)">上架</el-button>
          <el-button size="small" @click="openStockDialog(row)">修改库存</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="pageData.total" @change="loadList" />

    <el-dialog v-model="stockDialog.show" title="修改库存">
      <el-input-number v-model="stockDialog.stock" :min="0"></el-input-number>
      <template #footer>
        <el-button @click="stockDialog.show=false">取消</el-button>
        <el-button type="primary" @click="confirmStock">确认修改</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="detailDialog.show" title="商品详情" width="650px">
      <div v-if="detailData" style="line-height:2.2;">
        <p>商品ID：{{ detailData.id }}</p>
        <p>商品名称：{{ detailData.name }}</p>
        <p>商品图片展示（点击图片放大预览）：</p>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-image
            v-for="(img,idx) in detailData.imageUrls"
            :key="idx"
            :src="img"
            :preview-src-list="detailData.imageUrls"
            :initial-index="idx"
            style="width:120px"
            fit="contain"
          />
        </div>
        <p>价格：{{ detailData.price }}</p>
        <p>库存：{{ detailData.stock }}</p>
        <p>商品状态：{{ detailData.status }}</p>
        <p>种类：{{ detailData.categoryName }}</p>
        <p>二级分类ID：{{ detailData.categoryId }}</p>
        <p>发布时间：{{ detailData.publishedAt || '-' }}</p>
        <p>下架时间：{{ detailData.closedAt || '-' }}</p>
        <p>商品描述：</p>
        <div v-html="detailData.description || '无'"></div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.show=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getSellerProductList, freezeProduct, unfreezeProduct, offProduct, onProduct, updateStock, getSellerProductDetail } from '../../api/seller'

// 和接口参数一一对应：status / page / size
const query = ref({
  page: 1,
  size: 10,
  status: undefined
})

// 分页结果
const pageData = ref({
  list: [],
  total: 0
})

// 弹窗
const stockDialog = reactive({ show:false, id:null, stock:0 })
const detailDialog = reactive({ show: false })
const detailData = ref(null)

// 加载卖家商品列表
const loadList = async () => {
  // 正式联调打开下面
  const res = await getSellerProductList(query.value)
  pageData.value = res.data
}

// 查看详情
const openDetail = async (id) => {
  try {
    const res = await getSellerProductDetail(id)
    detailData.value = res.data
    detailDialog.show = true
  } catch (err) {
    ElMessage.error('无权限操作该商品')
  }
}

const freeze = async (id) => {
  await freezeProduct(id)
  ElMessage.success('已冻结')
  loadList()
}
const unfreeze = async (id) => {
  await unfreezeProduct(id)
  ElMessage.success('已解冻')
  loadList()
}
const offline = async (id) => {
  await offProduct(id)
  ElMessage.success('已下架')
  loadList()
}
const online = async (id) => {
  await onProduct(id)
  ElMessage.success('已上架')
  loadList()
}

const openStockDialog = (row) => {
  stockDialog.show = true
  stockDialog.id = row.id
  stockDialog.stock = row.stock
}
const confirmStock = async () => {
  await updateStock(stockDialog.id, stockDialog.stock)
  stockDialog.show = false
  ElMessage.success('库存已更新')
  loadList()
}

onMounted(()=> loadList())
</script>
