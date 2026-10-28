<template>
  <div>
    <div v-if="showNav" style="background:#222;color:#fff;padding:12px;display:flex;gap:16px;align-items:center;">
      <template v-if="userStore.buyerToken">
        <router-link to="/buyer/home" style="color:#fff;text-decoration:none">买家首页</router-link>
        <router-link to="/buyer/my-intent" style="color:#fff;text-decoration:none">我的历史意向</router-link>
        <el-button link @click="handleBuyerLogout" style="color:#fff">退出登录</el-button>
      </template>
      <template v-if="userStore.sellerToken">
        <router-link to="/seller/product-list" style="color:#fff;text-decoration:none">我的商品</router-link>
        <router-link to="/seller/publish" style="color:#fff;text-decoration:none">发布商品</router-link>
        <router-link to="/seller/category" style="color:#fff;text-decoration:none">分类管理</router-link>
        <router-link to="/seller/history-product" style="color:#fff;text-decoration:none">历史下架商品</router-link>
        <router-link to="/seller/customer" style="color:#fff;text-decoration:none">客户列表</router-link>
        <router-link to="/seller/modify-pwd" style="color:#fff;text-decoration:none">修改密码</router-link>

        <el-button link @click="handleSellerLogout" style="color:#fff">退出登录</el-button>
      </template>
    </div>
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/useUserStore'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showNav = computed(() => route.path !== '/')

const handleBuyerLogout = () => {
  userStore.buyerLogout()
  router.push('/')
}
const handleSellerLogout = () => {
  userStore.sellerLogout()
  router.push('/')
}
</script>
