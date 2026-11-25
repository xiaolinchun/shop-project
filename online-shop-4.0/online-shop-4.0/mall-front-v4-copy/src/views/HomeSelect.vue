<template>
  <div style="height:100vh;display:flex;flex-direction:column;">
    <div style="background:#222;color:#fff;padding:12px 24px;display:flex;align-items:center;gap:24px;font-size:16px;">
      <span>商城系统</span>
    </div>

    <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;background:#f5f7fa;">
      <h1 style="font-size:32px;color:#333;margin-bottom:36px;font-weight:500;">商城系统</h1>
      <div style="display:flex;gap:24px;">
        <el-button
          size="large"
          @click="$router.push('/buyer/login')"
          style="background:#2b6cb0;border-color:#2b6cb0;color:#fff;padding:10px 28px;font-size:15px;"
        >
          买家登录入口
        </el-button>

        <el-button
          size="large"
          @click="$router.push('/seller/login')"
          style="background:#38a169;border-color:#38a169;color:#fff;padding:10px 28px;font-size:15px;"
        >
          卖家登录入口
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup></script>
