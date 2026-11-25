<template>
  <div style="max-width:900px;margin:30px auto;padding:20px">
    <el-button @click="$router.push('/seller/product-list')">返回我的商品</el-button>
    <h2>发布商品</h2>
    <el-form :model="form" label-width="140px">
      <el-form-item label="商品名称">
        <el-input v-model="form.name"></el-input>
      </el-form-item>
      <el-form-item label="价格">
        <el-input-number v-model="form.price" :min="0.01" :precision="2"></el-input-number>
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="form.stock" :min="1"></el-input-number>
      </el-form-item>
      <el-form-item label="所属二级分类">
        <el-select v-model="form.categoryId" placeholder="选择分类">
          <el-option v-for="item in flatCategory" :key="item.id" :label="item.categoryName" :value="item.id"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="商品图片(最多9张)">
        <el-upload :http-request="customUpload" list-type="picture-card" :file-list="fileList" :limit="9" accept=".jpg,.png">
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="富文本描述">
        <el-input v-model="form.description" type="textarea" rows="6"></el-input>
        <div style="font-size:12px;color:#888">支持html富文本片段</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitPublish">发布商品</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { publishProduct, uploadProductImage, getSellerCategoryTree } from '../../api/seller'
const router = useRouter()
const form = ref({
  name: '',
  price: 0,
  stock: 1,
  categoryId: null,
  description: '',
  imageUrls: []
})
const fileList = ref([])
const categoryTree = ref([])
const flatCategory = ref([])

const customUpload = async (opt) => {
  const fd = new FormData()
  fd.append('file', opt.file)
  try {
    const res = await uploadProductImage(fd)
    const url = res.data
    fileList.value.push({ url })
    form.value.imageUrls.push(url)
  } catch(err) { opt.onError(err) }
}

const submitPublish = async () => {
  if(!form.value.name) return ElMessage.warning('请输入商品名称')
  if(!form.value.categoryId) return ElMessage.warning('请选择二级分类')
  await publishProduct(form.value)
  ElMessage.success('发布成功')
  router.push('/seller/product-list')
}

const loadCat = async () => {
  const res = await getSellerCategoryTree()
  categoryTree.value = res.data
  flatCategory.value = categoryTree.value.flatMap(p => p.children || [])
}
onMounted(()=> loadCat())
</script>
