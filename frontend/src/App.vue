<script setup lang="ts">
import { watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'

// 标签页标题跟随路由 + 应用标题 (配置异步加载完成后重算, 免得刷新页短暂显示默认值)
const route = useRoute()
const appStore = useAppStore()
watchEffect(() => {
  if (route.meta.title) {
    document.title = `${route.meta.title} - ${appStore.title}`
  }
})
</script>

<template>
  <router-view />
</template>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
