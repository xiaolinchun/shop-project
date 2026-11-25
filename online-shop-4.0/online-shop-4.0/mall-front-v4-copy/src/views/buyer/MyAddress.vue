<template>
  <div style="max-width:1100px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/home')">返回首页</el-button>
    <h2>收货地址</h2>
    <p class="hint">
      可配置多个收货地址并设置一个默认地址；下单时默认地址自动带入、可切换。
      修改 / 删除地址不影响历史订单的地址快照。
    </p>

    <!-- 地址簿业务组件（自包含增删改查 + 设为默认） -->
    <AddressBook />
  </div>
</template>

<script setup>
/**
 * MyAddress — 买家收货地址管理页面（FR-C08）
 *
 * 页面仅做布局与说明，地址的完整业务逻辑封装在 AddressBook 业务组件内。
 */
import AddressBook from '../../components/buyer/AddressBook.vue'
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; }
</style>
