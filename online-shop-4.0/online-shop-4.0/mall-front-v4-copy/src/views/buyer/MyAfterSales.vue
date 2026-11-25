<template>
  <div style="max-width:1100px;margin:30px auto;padding:0 20px">
    <el-button @click="$router.push('/buyer/orders')">返回我的订单</el-button>
    <h2>我的售后</h2>
    <p class="hint">
      自「开始发货」起可按订单明细发起售后；进展在此自助查看（系统不发送通知）。
      售后为平行单据，不改写订单状态。
    </p>

    <!-- 我的售后单列表业务组件（2.28） -->
    <AfterSalesTable ref="tableRef" />
  </div>
</template>

<script setup>
/**
 * MyAfterSales — 买家「我的售后单」页面（FR-C13 / 2.28）
 *
 * 页面仅做布局，列表加载 / 分页 / 状态渲染封装在 AfterSalesTable 业务组件内。
 */
import { ref } from 'vue'
import AfterSalesTable from '../../components/buyer/AfterSalesTable.vue'
const tableRef = ref(null)
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; }
</style>
