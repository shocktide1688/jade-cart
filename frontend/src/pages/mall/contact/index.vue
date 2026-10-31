<template>
  <div>
    <el-card>
      <template #header>
        <span>客服留言</span>
      </template>
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-select
          v-model="query.status"
          placeholder="状态"
          style="width:140px"
          clearable
          @change="load"
        >
          <el-option
            label="新留言"
            value="NEW"
          />
          <el-option
            label="处理中"
            value="PROCESSING"
          />
          <el-option
            label="已关闭"
            value="CLOSED"
          />
        </el-select>
        <el-button @click="load">
          查询
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="rows"
        border
      >
        <el-table-column
          prop="createdAt"
          label="时间"
          width="170"
        />
        <el-table-column
          prop="name"
          label="姓名"
          width="110"
        >
          <template #default="{ row }">
            {{ row.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="email"
          label="邮箱"
          min-width="160"
        />
        <el-table-column
          prop="topic"
          label="主题"
          width="100"
        />
        <el-table-column
          prop="message"
          label="内容"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="contactTag(row.status)">
              {{ contactLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="170"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'NEW'"
              link
              type="warning"
              @click="transition(row, 'processing')"
            >
              开始处理
            </el-button>
            <el-button
              v-if="row.status !== 'CLOSED'"
              link
              type="success"
              @click="transition(row, 'closed')"
            >
              关闭
            </el-button>
            <el-button
              v-if="row.status === 'CLOSED'"
              link
              type="info"
              @click="transition(row, 'reopen')"
            >
              重开
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        style="margin-top:12px"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, status: '' })

const contactTag = (s: string): any => ({ NEW: 'danger', PROCESSING: 'warning', CLOSED: 'info' }[s] || 'info')
const contactLabel = (s: string): string => ({ NEW: '新留言', PROCESSING: '处理中', CLOSED: '已关闭' }[s] || s)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/contact-messages', { params: query })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

async function transition(row: any, action: string) {
  await request.post(`/api/v1/contact-messages/${row.publicId}/${action}`)
  ElMessage.success('已更新')
  load()
}

onMounted(load)
</script>
