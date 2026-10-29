<template>
  <el-container style="max-width:900px;margin:30px auto;padding:10px 20px">
    <div>
    <el-page-header content="卖家管理后台"></el-page-header><br>
    <el-space  direction="vertical" style="margin-bottom:20px">
      <el-button @click="$router.push('/seller/history')">历史下架商品</el-button>
      <el-button @click="$router.push('/seller/password')">修改密码</el-button>
      <el-button type="danger" @click="logout">退出登录</el-button>
    </el-space>
    </div>

    <div>
    <el-card header="当前在售商品">
      <div v-if="currentProduct">
        <p>商品ID：{{ currentProduct.id }}</p>
        <p>名称：{{ currentProduct.name }}</p>
        <p>状态：{{ PRODUCT_STATUS[currentProduct.status] }}</p>
        <p>价格：¥{{ currentProduct.price }}</p>
        <el-space>
          <el-button @click="goIntentList(currentProduct.id)">查看意向队列</el-button>
          <el-button type="warning" @click="freeze(currentProduct.id)">冻结</el-button>
          <el-button type="success" @click="unfreeze(currentProduct.id)">解冻</el-button>
          <el-button type="danger" @click="offline(currentProduct.id)">下架</el-button>
        </el-space>
      </div>
      <div v-else>
        <el-empty description="无在售商品"></el-empty>
      </div>
    </el-card>
    
    <el-card header="发布新商品" style="margin-top:20px">
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="商品名称">
          <el-input v-model="publishForm.name"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="publishForm.description" type="textarea"></el-input>
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="publishForm.imageUrl"></el-input>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="publishForm.price" :min="0" step="0.01"></el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="publish" :loading="pubLoading">发布（会顶替现有在售商品）</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    </div>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentProduct, publishProduct, freezeProduct, unfreezeProduct, offProduct } from '../../api/mall'
import { PRODUCT_STATUS } from '../../constants'
import { ElMessage } from 'element-plus'

const router = useRouter()
const currentProduct = ref(null)
const pubLoading = ref(false)
const publishForm = ref({
  name: '',
  description: '',
  imageUrl: '',
  price: 0
})

async function loadCurr() {
  try {
    currentProduct.value = await getCurrentProduct()
  } catch (e) {}
}

onMounted(() => {
  //简单判断本地登录标记
  if (!localStorage.getItem('sellerUser')) {
    router.replace('/seller/login')
  }
  loadCurr()
})

function logout() {
  localStorage.removeItem('sellerUser')
  router.push('/seller/login')
  ElMessage.info('已退出')
}

async function publish() {
  if (!publishForm.value.name || !publishForm.value.price || publishForm.value.price <= 0) {
    return ElMessage.warning('商品名称必填,价格必须大于0')
  }
  pubLoading.value = true
  try {
    await publishProduct(publishForm.value)
    ElMessage.success('发布成功，已顶替旧商品')
    publishForm.value = { name: '', description: '', imageUrl: '', price: 0 }
    await loadCurr()
  } finally {
    pubLoading.value = false
  }
}

function goIntentList(id) {
  router.push(`/seller/intent/${id}`)
}

async function freeze(id) {
  await freezeProduct(id)
  ElMessage.success('已冻结')
  loadCurr()
}
async function unfreeze(id) {
  await unfreezeProduct(id)
  ElMessage.success('已解冻')
  loadCurr()
}
async function offline(id) {
  await offProduct(id)
  ElMessage.success('已下架')
  loadCurr()
}
</script>
