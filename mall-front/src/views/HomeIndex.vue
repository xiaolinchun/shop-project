<template>
  <div class="home-container">
    <div class="box">
      <h1>在线商城系统</h1>
      <div class="btn-group">
        <button @click="$router.push('/seller/login')" class="seller-btn">
          卖家入口（去登录）
        </button>
        <button @click="$router.push('/buyer')" class="buyer-btn">
          买家入口 
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}
.box {
  background: white;
  padding: 48px 60px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  text-align: center;
}
h1 {
  margin-bottom: 32px;
  color: #333;
}
.btn-group {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
button {
  padding: 14px 28px;
  font-size: 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.seller-btn {
  background-color: #409eff;
  color: white;
}
.buyer-btn {
  background-color: #67c23a;
  color: white;
}
</style>
