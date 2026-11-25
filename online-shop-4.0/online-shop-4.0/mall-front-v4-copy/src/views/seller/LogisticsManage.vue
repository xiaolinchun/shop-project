<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>物流管理</h2>
    <p class="hint">
      维护线下配送可选的快递物流服务商清单（如顺丰速运）；「我的商品 → 开始发货」时
      从这份清单选择服务商并登记运单号。清单由卖家自持，数量不限。
    </p>

    <!-- 物流服务商清单业务组件（3.25–3.27） -->
    <LogisticsProviderManager />
  </div>
</template>

<script setup>
/**
 * LogisticsManage — 卖家「物流管理」页面（FR-C10 / 3.25–3.27）
 *
 * 页面仅做布局与说明，服务商清单的查询 / 新增 / 删除逻辑封装在
 * LogisticsProviderManager 业务组件内部。
 */
import LogisticsProviderManager from '../../components/seller/LogisticsProviderManager.vue'
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; margin-bottom: 12px; }
</style>
