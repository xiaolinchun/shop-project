import { createRouter, createWebHistory } from 'vue-router'

const HomeSelect = () => import('../views/HomeSelect.vue')

// 买家页面
const BuyerLogin = () => import('../views/buyer/Login.vue')
const BuyerRegister = () => import('../views/buyer/Register.vue')
const BuyerHome = () => import('../views/buyer/Home.vue')
const BuyerProductDetail = () => import('../views/buyer/ProductDetail.vue')
const BuyerMyCart = () => import('../views/buyer/MyCart.vue')
const BuyerMyFavorite = () => import('../views/buyer/MyFavorite.vue')
const BuyerMyOrders = () => import('../views/buyer/MyOrders.vue')

// 卖家页面
const SellerLogin = () => import('../views/seller/Login.vue')
const SellerProductList = () => import('../views/seller/SellerProductList.vue')
const SellerPublish = () => import('../views/seller/ProductPublish.vue')
const SellerCategoryManage = () => import('../views/seller/CategoryManage.vue')
const SellerCustomerList = () => import('../views/seller/CustomerList.vue')
const SellerHistoryProduct = () => import('../views/seller/HistoryProduct.vue')
const SellerModifyPwd = () => import('../views/seller/ModifyPwd.vue')

const routes = [
  {
    path: '/',
    name: 'HomeSelect',
    component: HomeSelect
  },

  // ─── 买家路由组 ───
  { path: '/buyer/login', name: 'BuyerLogin', component: BuyerLogin },
  { path: '/buyer/register', name: 'BuyerRegister', component: BuyerRegister },
  { path: '/buyer/home', name: 'BuyerHome', component: BuyerHome, meta: { needBuyer: false } },
  { path: '/buyer/detail/:id', name: 'BuyerProductDetail', component: BuyerProductDetail, meta: { needBuyer: false } },
  { path: '/buyer/cart', name: 'BuyerMyCart', component: BuyerMyCart, meta: { needBuyer: true } },
  { path: '/buyer/favorite', name: 'BuyerMyFavorite', component: BuyerMyFavorite, meta: { needBuyer: true } },
  { path: '/buyer/orders', name: 'BuyerMyOrders', component: BuyerMyOrders, meta: { needBuyer: true } },

  // ─── 卖家路由组 ───
  { path: '/seller/login', name: 'SellerLogin', component: SellerLogin },
  { path: '/seller/product-list', name: 'SellerProductList', component: SellerProductList, meta: { needSeller: true } },
  { path: '/seller/publish', name: 'SellerPublish', component: SellerPublish, meta: { needSeller: true } },
  { path: '/seller/category', name: 'SellerCategoryManage', component: SellerCategoryManage, meta: { needSeller: true } },
  { path: '/seller/customer', name: 'SellerCustomerList', component: SellerCustomerList, meta: { needSeller: true } },
  { path: '/seller/history-product', name: 'SellerHistoryProduct', component: SellerHistoryProduct, meta: { needSeller: true } },
  { path: '/seller/modify-pwd', name: 'SellerModifyPwd', component: SellerModifyPwd, meta: { needSeller: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面校验 token
router.beforeEach(async (to, from, next) => {
  if (!to.meta.needBuyer && !to.meta.needSeller) {
    return next()
  }
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
