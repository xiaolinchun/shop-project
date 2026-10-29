<template>
  <el-container style="max-width:700px;margin:40px auto;padding:0 20px">
    <el-card header="查询购买意向">
      <el-form label-width="100px">
        <el-form-item label="口令码">
          <el-input v-model="token" placeholder="8位口令码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query" :loading="querying">查询</el-button>
          <el-button @click="$router.push('/buyer')">返回商品页</el-button>
        </el-form-item>
      </el-form>

      <div v-if="intentInfo">
        <el-divider></el-divider>
        <p>意向状态：{{ INTENT_STATUS[intentInfo.status] }}</p>
        <p>前面排队人数：{{ intentInfo.queueIndex }}</p>
        <p>意向ID:{{ intentInfo.intentId }}</p>

        <el-button @click="openEdit = true">修改姓名电话</el-button>
        <el-button type="danger" @click="handleCancel">撤销意向</el-button>
      </div>
    </el-card>

    <!-- 修改弹窗 -->
    <el-dialog v-model="openEdit" title="修改信息">
      <el-form :model="editForm">
        <el-form-item label="姓名">
          <el-input v-model="editForm.buyerName"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openEdit = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { queryIntentByToken, modifyIntent, cancelIntent } from '../../api/mall'
import { INTENT_STATUS } from '../../constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const token = ref('')
const querying = ref(false)
const intentInfo = ref(null)
const openEdit = ref(false)
const editForm = ref({ buyerName: '', phone: '' })

onMounted(() => {
  const localToken = localStorage.getItem('buyerToken')
  if (localToken) {
    token.value = localToken
  }
})

async function query() {
  if (!token.value) return ElMessage.warning('请输入口令码')
  querying.value = true
  try {
    intentInfo.value = await queryIntentByToken(token.value)
  } finally {
    querying.value = false
  }
}

async function submitEdit() {
  if (!editForm.value.buyerName || !editForm.value.phone) {
    return ElMessage.warning('姓名手机号不能为空')
  }
  try {
    await modifyIntent({
      token: token.value,
      ...editForm.value
    })
    ElMessage.success('修改成功')
    openEdit.value = false
    await query()
  } catch (e) {}
}

async function handleCancel() {
  await ElMessageBox.confirm('确认撤销该购买意向？')
  try {
    await cancelIntent(token.value)
    ElMessage.success('已撤销')
    localStorage.removeItem('buyerToken')
    intentInfo.value = null
  } catch (e) {}
}
</script>
