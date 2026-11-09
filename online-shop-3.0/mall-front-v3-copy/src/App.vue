<template>
  <div>
    <!-- 顶部导航栏 -->
    <div v-if="showNav" style="background:#222;color:#fff;padding:12px 24px;display:flex;gap:16px;align-items:center;flex-wrap:wrap;">
      <span style="font-size:16px;font-weight:bold;margin-right:8px">商城系统</span>

      <!-- 买家导航 -->
      <template v-if="userStore.buyerToken">
        <router-link to="/buyer/home" :class="navClass">买家首页</router-link>
        <router-link to="/buyer/cart" :class="navClass">购物车</router-link>
        <router-link to="/buyer/favorite" :class="navClass">我的收藏</router-link>
        <router-link to="/buyer/orders" :class="navClass">我的订单</router-link>
        <el-button link @click="handleBuyerLogout" style="color:#fff">退出登录</el-button>
      </template>

      <!-- 卖家导航 -->
      <template v-if="userStore.sellerToken">
        <router-link to="/seller/product-list" :class="navClass">我的商品</router-link>
        <router-link to="/seller/publish" :class="navClass">发布商品</router-link>
        <router-link to="/seller/category" :class="navClass">分类管理</router-link>
        <router-link to="/seller/history-product" :class="navClass">已下架商品</router-link>
        <router-link to="/seller/customer" :class="navClass">客户列表</router-link>
        <router-link to="/seller/modify-pwd" :class="navClass">修改密码</router-link>
        <el-button link @click="handleSellerLogout" style="color:#fff">退出登录</el-button>
      </template>
    </div>

    <router-view />
  </div>
</template>

<script setup>
/**
 * App — 根组件
 *
 * v3.0 导航更新：
 *   买家侧：首页 / 购物车 / 收藏 / 订单（移除「我的历史意向」）
 *   卖家侧：商品 / 发布 / 分类 / 已下架 / 客户 / 密码（移除「意向队列」「匿名意向」）
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/useUserStore'

const route     = useRoute()
const router    = useRouter()
const userStore = useUserStore()

const showNav = computed(() => route.path !== '/')

const navClass = 'nav-link'

const handleBuyerLogout = () => {
  userStore.buyerLogout()
  router.push('/')
}
const handleSellerLogout = () => {
  userStore.sellerLogout()
  router.push('/')
}
</script>

<style scoped>
.nav-link {
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.nav-link:hover {
  background: rgba(255,255,255,0.15);
}
</style>
