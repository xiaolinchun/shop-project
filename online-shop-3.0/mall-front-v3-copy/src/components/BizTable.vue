<template>
  <div class="biz-table">
    <el-table :data="data" border v-loading="loading" style="width:100%">
      <slot />
    </el-table>
    <div class="biz-table__pagination" v-if="total > 0">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        @update:current-page="onPageChange"
        @update:page-size="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
/**
 * BizTable — 通用业务表格基础组件
 *
 * 在 Element Plus <el-table> + <el-pagination> 之上做了一层薄封装，
 * 统一管理分页、加载态，各业务列表组件只需关心列定义与数据获取。
 *
 * Props
 *   data     : 当前页行数据
 *   total    : 总条数
 *   page     : 当前页码（v-model）
 *   pageSize : 每页条数（v-model）
 *   loading  : 加载状态
 *
 * Events
 *   update:page / update:pageSize — 分页变化
 */
defineProps({
  data:     { type: Array,  default: () => [] },
  total:    { type: Number, default: 0 },
  page:     { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  loading:  { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const onPageChange = (p) => emit('update:page', p)
const onSizeChange = (s) => {
  emit('update:pageSize', s)
  emit('update:page', 1)   // 切换每页条数时自动回到第 1 页
}
</script>

<style scoped>
.biz-table__pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
