<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>备份管理</span>
          <el-button type="primary" :loading="running" @click="run">立即备份</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
                title="备份内容: manifest + 全部商城表 JSON 快照 + 媒体文件; 每日 04:00 自动备份, 本地保留最近 7 份。" />
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="filename" label="文件" min-width="260" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ (row.sizeBytes / 1024).toFixed(1) }} KB</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <a :href="'/api/v1/backups/' + row.filename" target="_blank">
              <el-button link type="primary">下载</el-button>
            </a>
            <el-popconfirm title="确定删除该备份?" @confirm="remove(row.filename)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const loading = ref(false)
const running = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = (await request.get('/api/v1/backups')).data.data
  } finally { loading.value = false }
}

async function run() {
  running.value = true
  try {
    const res: any = await request.post('/api/v1/backups')
    ElMessage.success(`备份完成: ${res.data.data.filename}`)
    load()
  } finally { running.value = false }
}

async function remove(filename: string) {
  await request.delete(`/api/v1/backups/${filename}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
