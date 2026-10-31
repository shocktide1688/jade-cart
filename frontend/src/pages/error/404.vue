<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" style="background: #00a86b; border-color: #00a86b;" @click="router.push('/')">
      返回首页
    </el-button>
  </div>
</template>

<style scoped>
.error-page {
  height: 100vh;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: #f5f7fa;
}
h1 { font-size: 96px; margin: 0; color: #00a86b; }
p { color: #909399; margin-bottom: 24px; }
</style>
