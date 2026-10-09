import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path:'/', component: ()=>import('../views/HomeIndex.vue') },
  { path:'/buyer', component: ()=>import('../views/buyer/BuyerHome.vue') },
  { path:'/buyer/query', component: ()=>import('../views/buyer/BuyerQuery.vue') },
  { path:'/seller/login', component: ()=>import('../views/seller/SellerLogin.vue') },
  { path:'/seller/home', component: ()=>import('../views/seller/SellerHome.vue') },
  { path:'/seller/intent/:productId', component: ()=>import('../views/seller/IntentList.vue') },
  { path:'/seller/history', component: ()=>import('../views/seller/HistoryProduct.vue') },
  { path:'/seller/password', component: ()=>import('../views/seller/ModifyPwd.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})
export default router
