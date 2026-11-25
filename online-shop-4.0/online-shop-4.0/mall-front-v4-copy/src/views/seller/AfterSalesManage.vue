<template>
  <div style="max-width:1100px;margin:30px auto;padding:20px">
    <h2>售后管理</h2>
    <p class="hint">
      买家发起的售后申请在此处理：标记协商中 → 录入结果（退货退款 / 仅退款 / 换货 / 驳回）→
      按结果确认收到退回件或登记换货寄出。协商在线下进行，系统只记录申请与最终结果。
    </p>

    <!-- 售后处理业务组件（3.28–3.32）：列表 + 行内状态机动作 + 处理弹窗 -->
    <AfterSalesManageTable ref="tableRef" />
  </div>
</template>

<script setup>
/**
 * AfterSalesManage — 卖家「售后管理」页面（FR-C13~C16 / 3.28–3.32）
 *
 * 页面仅做布局，售后单列表与处理动作（协商 / 结果 / 退回 / 换货）全部封装在
 * AfterSalesManageTable 业务组件内部。
 */
import { ref } from 'vue'
import AfterSalesManageTable from '../../components/seller/AfterSalesManageTable.vue'
const tableRef = ref(null)
</script>

<style scoped>
.hint { color: #909399; font-size: 13px; margin-bottom: 12px; }
</style>
