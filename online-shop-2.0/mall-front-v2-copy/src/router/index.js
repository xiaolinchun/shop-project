import { createRouter, createWebHistory } from 'vue-router'

const HomeSelect = () => import('../views/HomeSelect.vue')

// 买家页面
const BuyerLogin = () => import('../views/buyer/Login.vue')
const BuyerRegister = () => import('../views/buyer/Register.vue')
const BuyerHome = () => import('../views/buyer/Home.vue')
const BuyerProductDetail = () => import('../views/buyer/ProductDetail.vue')
const BuyerMyIntent = () => import('../views/buyer/MyIntent.vue')

// 卖家页面
const SellerLogin = () => import('../views/seller/Login.vue')
const SellerProductList = () => import('../views/seller/SellerProductList.vue')
const SellerPublish = () => import('../views/seller/ProductPublish.vue')
const SellerCategoryManage = () => import('../views/seller/CategoryManage.vue')
const SellerIntentQueue = () => import('../views/seller/IntentQueue.vue')
const SellerCustomerList = () => import('../views/seller/CustomerList.vue')
const SellerAnonymousIntent = () => import('../views/seller/AnonymousIntent.vue')
const SellerHistoryProduct = () => import('../views/seller/HistoryProduct.vue')
const SellerModifyPwd = () => import('../views/seller/ModifyPwd.vue')

const routes = [
  {
    path: '/',
    name: 'HomeSelect',
    component: HomeSelect
  },
  // 买家路由组
  { path: '/buyer/login', name: 'BuyerLogin', component: BuyerLogin },
  { path: '/buyer/register', name: 'BuyerRegister', component: BuyerRegister },
  { path: '/buyer/home', name: 'BuyerHome', component: BuyerHome, meta: { needBuyer: false } },
  { path: '/buyer/detail/:id', name: 'BuyerProductDetail', component: BuyerProductDetail, meta: { needBuyer: false } },
  { path: '/buyer/my-intent', name: 'BuyerMyIntent', component: BuyerMyIntent, meta: { needBuyer: true } },

  // 卖家路由组
  { path: '/seller/login', name: 'SellerLogin', component: SellerLogin },
  { path: '/seller/product-list', name: 'SellerProductList', component: SellerProductList, meta: { needSeller: true } },
  { path: '/seller/publish', name: 'SellerPublish', component: SellerPublish, meta: { needSeller: true } },
  { path: '/seller/category', name: 'SellerCategoryManage', component: SellerCategoryManage, meta: { needSeller: true } },
  { path: '/seller/intent-queue/:productId', name: 'SellerIntentQueue', component: SellerIntentQueue, meta: { needSeller: true } },
  { path: '/seller/customer', name: 'SellerCustomerList', component: SellerCustomerList, meta: { needSeller: true } },
  { path: '/seller/anonymous-intent', name: 'SellerAnonymousIntent', component: SellerAnonymousIntent, meta: { needSeller: true } },
  { path: '/seller/history-product', name: 'SellerHistoryProduct', component: SellerHistoryProduct },
  { path: '/seller/modify-pwd', name: 'SellerModifyPwd', component: SellerModifyPwd },

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ✅ 修复：不在守卫里直接调用useUserStore，改用【页面内判断】或者延迟加载
// 方案：把鉴权逻辑改成，进入页面后在组件里校验token；
// 如果你一定要全局守卫，正确写法：先import pinia
router.beforeEach(async (to, from, next) => {
  // 先放行不需要登录的页面
  if (!to.meta.needBuyer && !to.meta.needSeller) {
    return next()
  }
  // 动态导入，保证pinia已经初始化
  const { useUserStore } = await import('../stores/useUserStore')
  const userStore = useUserStore()

  if (to.meta.needBuyer) {
    if (userStore.buyerToken) {
      next()
    } else {
      next('/')
    }
  } else if (to.meta.needSeller) {
    if (userStore.sellerToken) {
      next()
    } else {
      next('/')
    }
  } else {
    next()
  }
})

export default router
