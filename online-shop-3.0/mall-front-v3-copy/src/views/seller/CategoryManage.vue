<template>
  <div style="max-width:800px;margin:30px auto;padding:20px">
    <h2>分类管理</h2>
    <el-button type="primary" @click="openAddRoot">新增一级分类</el-button>

    <div v-for="p in catTree" :key="p.id" style="margin:16px 0;border:1px #eee solid;padding:12px;border-radius:6px">
      <div style="display:flex;align-items:center;gap:10px">
        <span><strong>{{p.categoryName}}</strong> (商品数:{{parentCount(p)}})</span>
        <el-button size="small" @click="openRename(p)">重命名</el-button>
        <el-button size="small" type="primary" @click="addChild(p.id)">新增子二级分类</el-button>
        <el-button size="small" type="danger" @click="delCat(p.id)">删除</el-button>
      </div>
      <div style="margin-left:24px;margin-top:8px">
        <div v-for="child in p.children" :key="child.id" style="display:flex;gap:10px;margin:4px 0">
          <span>- {{child.categoryName}} (商品数:{{child.productCount}})</span>
          <el-button size="small" @click="openRename(child)">重命名</el-button>
          <el-button size="small" type="danger" @click="delCat(child.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="addDialog.show" title="新增分类">
      <el-input v-model="addDialog.name" placeholder="分类名称"></el-input>
      <template #footer>
        <el-button @click="addDialog.show=false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameDialog.show" title="重命名分类">
      <el-input v-model="renameDialog.name"></el-input>
      <template #footer>
        <el-button @click="renameDialog.show=false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSellerCategoryTree, addCategory, renameCategory, delCategory } from '../../api/seller'
const catTree = ref([])
const addDialog = ref({ show:false, parentId:0, name:'' })
const renameDialog = ref({ show:false, id:null, name:'' })

const loadTree = async () => {
  const res = await getSellerCategoryTree()
  // “未分类”是历史商品迁移的兜底分类，由系统自动维护，不在管理页展示
  catTree.value = (res.data || []).filter(p => !(p.parentId === 0 && p.categoryName === '未分类'))
}
const confirmAdd = async () => {
  if(!addDialog.value.name) return ElMessage.warning('输入名称')
  const res = await addCategory({ parentId: addDialog.value.parentId, categoryName: addDialog.value.name })
  if (res.code !== 200) return ElMessage.error(res.msg || '新增失败')
  addDialog.value.show = false
  addDialog.value.name = ''
  loadTree()
  ElMessage.success('新增成功')
}
const openAddRoot = () => {
  // 一级分类约定 parentId=0（后端 @NotNull 校验不接受 null）；必须重置，否则会残留上次 addChild 的父 id
  addDialog.value = { show:true, parentId:0, name:'' }
}
const parentCount = (p) => {
  // 商品只挂二级分类，一级分类展示子分类商品数之和
  return (p.children || []).reduce((sum, c) => sum + (c.productCount || 0), 0)
}
const addChild = (pid) => {
  addDialog.value = { show:true, parentId: pid, name:'' }
}
const openRename = (item) => {
  renameDialog.value = { show:true, id: item.id, name: item.categoryName }
}
const confirmRename = async () => {
  const res = await renameCategory(renameDialog.value.id, renameDialog.value.name)
  if (res.code !== 200) return ElMessage.error(res.msg || '重命名失败')
  renameDialog.value.show = false
  loadTree()
  ElMessage.success('重命名完成')
}
const delCat = async (id) => {
  const res = await delCategory(id)
  if (res.code !== 200) return ElMessage.error(res.msg || '删除失败')
  loadTree()
  ElMessage.success('已删除')
}
onMounted(()=> loadTree())
</script>
