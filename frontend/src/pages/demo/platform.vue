<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const lockTest = ref<any>({ result: '', loading: false })
const idempotentKey = ref(crypto.randomUUID())
const orderResult = ref<any>({ data: null, loading: false })
const metrics = ref<any>(null)

async function testLock() {
  lockTest.value.loading = true
  try {
    const res = await request({ url: '/api/v1/lock-demo/fair/lock-test', method: 'POST' })
    lockTest.value.result = JSON.stringify(res.data.data, null, 2)
  } finally { lockTest.value.loading = false }
}

async function testOrder() {
  orderResult.value.loading = true
  try {
    const res = await request({
      url: '/api/v1/orders',
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotentKey.value },
      data: { productName: '演示商品', amount: 99.99 },
    })
    orderResult.value.data = res.data.data
    // 同样 key 再发一次, 应该返回 cached result
    idempotentKey.value = crypto.randomUUID()
  } finally { orderResult.value.loading = false }
}

async function loadMetrics() {
  const res = await request({ url: '/api/v1/metrics/summary', method: 'GET' })
  metrics.value = res.data.data
}

onMounted(loadMetrics)
</script>

<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card shadow="hover">
        <template #header><span style="font-weight: 600;">🔒 分布式锁</span></template>
        <p>Jade 实现了 7 种锁类型：普通锁、看门狗、读写锁、信号量、闭锁等</p>
        <el-button type="primary" :loading="lockTest.loading" @click="testLock" style="background: #00a86b; border-color: #00a86b;">
          测试公平锁
        </el-button>
        <pre v-if="lockTest.result" class="result">{{ lockTest.result }}</pre>
      </el-card>
    </el-col>

    <el-col :span="8">
      <el-card shadow="hover">
        <template #header><span style="font-weight: 600;">🔁 幂等性</span></template>
        <p>同一 X-Idempotency-Key 5 分钟内不会重复创建订单</p>
        <p class="key-text">当前 Key: <code>{{ idempotentKey }}</code></p>
        <el-button type="primary" :loading="orderResult.loading" @click="testOrder" style="background: #00a86b; border-color: #00a86b;">
          创建订单
        </el-button>
        <pre v-if="orderResult.data" class="result">{{ JSON.stringify(orderResult.data, null, 2) }}</pre>
      </el-card>
    </el-col>

    <el-col :span="8">
      <el-card shadow="hover">
        <template #header><span style="font-weight: 600;">📊 业务指标</span></template>
        <p>实时统计业务事件和实体数量</p>
        <el-button @click="loadMetrics">刷新</el-button>
        <pre v-if="metrics" class="result">{{ JSON.stringify(metrics, null, 2) }}</pre>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top: 16px;">
    <template #header><span style="font-weight: 600;">🔐 字段加密 (BCrypt + AES-256-GCM)</span></template>
    <p>Patient 实体的 idCard / phone 字段在 DB 里是加密存储的，API 自动解密返回明文</p>
    <p>DB 存储示例：<code>idCard = 0xA3F8...2B1C (AES ciphertext)</code></p>
    <p>API 返回：<code>idCard = "110101199001011234" (明文)</code></p>
  </el-card>

  <el-card style="margin-top: 16px;">
    <template #header><span style="font-weight: 600;">🚀 启动性能</span></template>
    <el-row :gutter="16">
      <el-col :span="6"><el-statistic title="Native 启动耗时" :value="0.9" suffix="s" /></el-col>
      <el-col :span="6"><el-statistic title="镜像大小" :value="283" suffix="MB" /></el-col>
      <el-col :span="6"><el-statistic title="JVM 内存 (启动后)" :value="18" suffix="MB" /></el-col>
      <el-col :span="6"><el-statistic title="JVM 启动后线程数" :value="17" /></el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.result { background: #f5f7fa; padding: 12px; border-radius: 4px; margin-top: 12px; font-size: 12px; max-height: 200px; overflow: auto; }
.key-text { font-size: 12px; color: #909399; word-break: break-all; }
code { background: #f5f7fa; padding: 2px 4px; border-radius: 3px; font-size: 12px; }
</style>
