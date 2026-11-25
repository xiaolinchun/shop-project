<template>
  <div class="review-list" v-loading="loading">
    <!-- 聚合评分（实时计算只读值，FR-C22） -->
    <div class="review-list__summary">
      <div class="review-list__score">
        <span class="review-list__num">{{ (Number(averageRating) || 0).toFixed(1) }}</span>
        <el-rate :model-value="Number(averageRating) || 0" disabled allow-half size="large" />
      </div>
      <div class="muted">共 {{ reviewCount }} 条评价</div>
    </div>

    <el-divider style="margin:12px 0" />

    <!-- 评价条目 -->
    <div v-if="list.length">
      <div v-for="r in list" :key="r.reviewId" class="review-item">
        <div class="review-item__head">
          <span class="review-item__name">{{ r.buyerMaskedName || '匿名买家' }}</span>
          <el-rate :model-value="r.rating" disabled size="small" />
          <span class="muted review-item__time">{{ r.createdAt }}</span>
        </div>
        <div v-if="r.content" class="review-item__content">{{ r.content }}</div>
        <div v-if="r.images && r.images.length" class="review-item__images">
          <el-image
            v-for="(img, i) in r.images" :key="i" :src="img"
            :preview-src-list="r.images" :initial-index="i"
            style="width:80px;height:80px" fit="cover"
          />
        </div>
      </div>

      <div v-if="total > size" class="review-list__pager">
        <el-pagination
          layout="prev, pager, next" :total="total"
          :current-page="page" :page-size="size" small
          @current-change="onPage"
        />
      </div>
    </div>
    <el-empty v-else-if="!loading" :image-size="60" description="暂无评价" />
  </div>
</template>

<script setup>
/**
 * ProductReviewList — 商品评价列表业务组件（FR-C22 / 2.30）
 *
 * 无需登录即可浏览；封装聚合评分（平均分 + 评价数）+ 分页评价列表，
 * 买家昵称脱敏展示（首字 + **）。productId 变化时自动重载。
 */
import { ref, watch } from 'vue'
import { getProductReviews } from '../../api/buyer'

const props = defineProps({
  productId: { type: [Number, String], required: true }
})

const list          = ref([])
const total         = ref(0)
const page          = ref(1)
const size          = ref(10)
const averageRating = ref(0)
const reviewCount   = ref(0)
const loading       = ref(false)

const load = async () => {
  if (!props.productId) return
  loading.value = true
  try {
    const res = await getProductReviews(props.productId, { page: page.value, size: size.value })
    if (res?.code === 200 && res.data) {
      list.value          = res.data.list || []
      total.value         = res.data.total || 0
      averageRating.value = res.data.averageRating || 0
      reviewCount.value   = res.data.reviewCount || 0
    } else {
      list.value = []; total.value = 0
    }
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const onPage = (p) => { page.value = p; load() }

watch(() => props.productId, () => { page.value = 1; load() }, { immediate: true })
defineExpose({ refresh: load })
</script>

<style scoped>
.review-list__summary { display: flex; align-items: center; gap: 20px; }
.review-list__score   { display: flex; align-items: center; gap: 10px; }
.review-list__num     { font-size: 32px; font-weight: bold; color: #e6a23c; }
.review-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.review-item__head { display: flex; align-items: center; gap: 12px; }
.review-item__name { font-weight: bold; }
.review-item__time { margin-left: auto; }
.review-item__content { margin: 6px 0; color: #303133; }
.review-item__images { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }
.review-list__pager { margin-top: 12px; display: flex; justify-content: center; }
.muted { color: #909399; font-size: 12px; }
</style>
