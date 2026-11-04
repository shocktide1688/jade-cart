<template>
  <div>
    <el-card>
      <template #header>
        <span>数据导入</span>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:16px"
        title="流程: 上传干跑预览 → 确认执行 (同文件 sha256 幂等, 重复确认被拒)"
      />
      <el-form inline>
        <el-form-item label="格式">
          <el-radio-group v-model="format">
            <el-radio value="generic">
              通用 (title,slug,sku,price,stock)
            </el-radio>
            <el-radio value="shopify">
              Shopify 导出 (Handle 分组)
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-upload
            :show-file-list="false"
            accept=".csv"
            :auto-upload="false"
            :on-change="onFile"
          >
            <el-button
              type="primary"
              :loading="uploading"
            >
              选择 CSV 上传预览
            </el-button>
          </el-upload>
        </el-form-item>
      </el-form>

      <template v-if="job">
        <el-descriptions
          :column="4"
          border
          size="small"
          style="margin-bottom:12px"
        >
          <el-descriptions-item label="任务">
            {{ job.publicId.slice(0, 8) }}…
          </el-descriptions-item>
          <el-descriptions-item label="总行数">
            {{ job.totalRows }}
          </el-descriptions-item>
          <el-descriptions-item label="商品数">
            {{ job.productCount }}
          </el-descriptions-item>
          <el-descriptions-item label="错误行">
            <span :style="job.errorRows > 0 ? 'color:#b42318;font-weight:600' : ''">{{ job.errorRows }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="job.errorRows > 0"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom:12px"
        >
          <div
            v-for="(e, i) in jobErrors"
            :key="i"
          >
            行 {{ e.row }}: {{ e.message }}
          </div>
        </el-alert>

        <el-button
          v-if="job.status === 'PREVIEW'"
          type="success"
          :loading="confirming"
          @click="confirm"
        >
          确认执行导入 ({{ job.productCount }} 个商品)
        </el-button>
        <el-tag
          v-else-if="job.status === 'CONFIRMED'"
          type="success"
        >
          已导入 {{ job.importedCount }} 个商品
        </el-tag>
      </template>

      <h4 style="margin-top:24px">
        导入历史
      </h4>
      <el-table
        :data="jobs"
        size="small"
        border
      >
        <el-table-column
          prop="createdAt"
          label="时间"
          width="170"
        />
        <el-table-column
          prop="filename"
          label="文件"
          min-width="160"
        />
        <el-table-column
          prop="sourceFormat"
          label="格式"
          width="90"
        />
        <el-table-column
          prop="productCount"
          label="商品"
          width="70"
        />
        <el-table-column
          prop="errorRows"
          label="错误行"
          width="80"
        />
        <el-table-column
          prop="importedCount"
          label="已导入"
          width="80"
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="importTag(row.status)">
              {{ importLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallImport' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const format = ref('generic')
const uploading = ref(false)
const confirming = ref(false)
const job = ref<any>(null)
const jobs = ref<any[]>([])

const jobErrors = ref<any[]>([])

const importTag = (s: string): any => ({ PREVIEW: 'info', CONFIRMED: 'success', FAILED: 'danger' }[s] || 'info')
const importLabel = (s: string): string => ({ PREVIEW: '待确认', CONFIRMED: '已导入', FAILED: '失败' }[s] || s)

async function onFile(file: any) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    fd.append('format', format.value)
    const res: any = await request.post('/api/v1/import/preview', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    job.value = res.data.data
    jobErrors.value = parseErrors(job.value.previewJson)
    if (job.value.status === 'CONFIRMED') {
      ElMessage.warning('该文件此前已导入过 (sha256 幂等)')
    } else {
      ElMessage.success(`解析完成: ${job.value.productCount} 个商品, ${job.value.errorRows} 个错误行`)
    }
    loadJobs()
  } finally { uploading.value = false }
}

function parseErrors(previewJson: string) {
  try {
    return JSON.parse(previewJson).errors || []
  } catch {
    return []
  }
}

async function confirm() {
  confirming.value = true
  try {
    const res: any = await request.post(`/api/v1/import/${job.value.publicId}/confirm`)
    job.value = res.data.data
    ElMessage.success(`已导入 ${job.value.importedCount} 个商品`)
    loadJobs()
  } finally { confirming.value = false }
}

async function loadJobs() {
  const res: any = await request.get('/api/v1/import/jobs')
  jobs.value = res.data.data
}

onMounted(loadJobs)
</script>
