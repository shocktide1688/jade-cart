<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>商品管理</span>
          <el-button type="primary" @click="openCreate">新建商品</el-button>
        </div>
      </template>
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-input v-model="query.keyword" placeholder="标题搜索" style="width:220px" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" style="width:130px" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="上架" value="ACTIVE" />
          <el-option label="归档" value="ARCHIVED" />
        </el-select>
        <el-button @click="load">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="slug" label="Slug" min-width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'info' : 'warning'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="起价" width="100">
          <template #default="{ row }">{{ money(row.minPriceCents) }}</template>
        </el-table-column>
        <el-table-column prop="variantCount" label="变体数" width="80" />
        <el-table-column prop="totalAvailable" label="可售" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-popconfirm title="归档后店面不再展示, 确定?" @confirm="archive(row.id)">
              <template #reference><el-button link type="danger">归档</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" v-model:current-page="query.page" @current-change="load" />
    </el-card>

    <!-- 新建商品 -->
    <el-dialog v-model="createVisible" title="新建商品" width="520px">
      <el-form :model="createForm" label-width="100">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="商品标题" />
        </el-form-item>
        <el-form-item label="Slug">
          <el-input v-model="createForm.slug" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="默认价格(分)">
          <el-input-number v-model="createForm.defaultPriceCents" :min="0" />
        </el-form-item>
        <el-form-item label="默认 SKU">
          <el-input v-model="createForm.defaultSku" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="直接上架">
          <el-switch v-model="createForm.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <!-- 商品详情: 变体轴 / 变体 / 库存 -->
    <el-drawer v-model="detailVisible" :title="detail?.title || '商品详情'" size="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="状态">
            <el-select v-model="detail.status" style="width:120px" @change="saveBasic">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="上架" value="ACTIVE" />
              <el-option label="归档" value="ARCHIVED" />
            </el-select>
          </el-descriptions-item>
          <el-descriptions-item label="首页精选">
            <el-switch v-model="detail.featured" @change="saveBasic" />
          </el-descriptions-item>
          <el-descriptions-item label="Slug" :span="2">{{ detail.slug }}</el-descriptions-item>
        </el-descriptions>

        <h4>主图</h4>
        <div style="display:flex;gap:12px;align-items:center;margin-bottom:16px">
          <img v-if="detail.mainImagePath" :src="'/media/' + detail.mainImagePath"
               style="width:80px;height:80px;object-fit:cover;border-radius:6px;border:1px solid #eee" />
          <div v-else class="ph" style="width:80px;height:80px;border-radius:6px;display:flex;align-items:center;justify-content:center;color:#ccc;background:#f4f4f4">✦</div>
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/jpeg,image/png,image/webp"
                     :on-change="uploadImage" :disabled="uploading">
            <el-button :loading="uploading">{{ detail.mainImagePath ? '更换主图' : '上传主图' }}</el-button>
          </el-upload>
          <el-button v-if="detail.mainImagePath" link type="danger" @click="removeImage">移除</el-button>
        </div>

        <h4>变体轴</h4>
        <div v-for="opt in detail.options" :key="opt.id" style="margin-bottom:8px;display:flex;gap:8px;align-items:center">
          <el-tag>{{ opt.displayName }}</el-tag>
          <el-tag v-for="v in opt.values" :key="v" type="info" size="small">{{ v }}</el-tag>
          <el-button link type="danger" size="small" @click="removeOption(opt.id)">删除轴</el-button>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:16px">
          <el-input v-model="newOption.key" placeholder="轴 key, 如 color" style="width:140px" />
          <el-input v-model="newOption.values" placeholder="取值, 逗号分隔: Red,Blue" style="width:220px" />
          <el-button @click="addOption">加轴</el-button>
          <el-button type="primary" plain :disabled="!detail.options.length" @click="generate">生成变体矩阵</el-button>
        </div>

        <h4>变体 / 库存</h4>
        <el-table :data="detail.variants" size="small" border>
          <el-table-column prop="sku" label="SKU" min-width="130" />
          <el-table-column prop="spec" label="规格" min-width="110" />
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.priceCents" :min="0" :controls="false" size="small"
                               style="width:100px" @change="saveVariant(row)" />
            </template>
          </el-table-column>
          <el-table-column label="在库" width="90">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :controls="false" size="small"
                               style="width:70px" @change="saveInventory(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="reserved" label="占用" width="60" />
          <el-table-column prop="available" label="可售" width="60" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const createVisible = ref(false)
const saving = ref(false)
const createForm = reactive({ title: '', slug: '', defaultPriceCents: 0, defaultSku: '', description: '', active: true })
const detailVisible = ref(false)
const detail = ref<any>(null)
const uploading = ref(false)
const newOption = reactive({ key: '', values: '' })

const money = (c: number) => '$' + (c / 100).toFixed(2)
const statusText = (s: string) => ({ DRAFT: '草稿', ACTIVE: '上架', ARCHIVED: '归档' }[s] || s)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/products', { params: query })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(createForm, { title: '', slug: '', defaultPriceCents: 0, defaultSku: '', description: '', active: true })
  createVisible.value = true
}

async function create() {
  if (!createForm.title) return ElMessage.warning('请输入标题')
  saving.value = true
  try {
    await request.post('/api/v1/products', {
      title: createForm.title, slug: createForm.slug || undefined,
      description: createForm.description || undefined,
      defaultPriceCents: createForm.defaultPriceCents, defaultSku: createForm.defaultSku || undefined,
      status: createForm.active ? 'ACTIVE' : 'DRAFT',
    })
    ElMessage.success('已创建')
    createVisible.value = false
    load()
  } finally { saving.value = false }
}

async function openDetail(id: number) {
  const res: any = await request.get(`/api/v1/products/${id}`)
  detail.value = res.data.data
  detailVisible.value = true
}

async function saveBasic() {
  await request.put(`/api/v1/products/${detail.value.id}`, {
    status: detail.value.status, featured: detail.value.featured,
  })
  ElMessage.success('已保存')
  load()
}

async function archive(id: number) {
  await request.delete(`/api/v1/products/${id}`)
  ElMessage.success('已归档')
  load()
}

async function uploadImage(file: any) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await request.post('/api/v1/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const path = res.data.data.path
    await request.put(`/api/v1/products/${detail.value.id}`, { mainImagePath: path })
    ElMessage.success('主图已更新')
    openDetail(detail.value.id)
  } finally { uploading.value = false }
}

async function removeImage() {
  await request.put(`/api/v1/products/${detail.value.id}`, { mainImagePath: '' })
  ElMessage.success('已移除主图')
  openDetail(detail.value.id)
}

async function addOption() {
  if (!newOption.key) return ElMessage.warning('请输入轴 key')
  const values = newOption.values.split(',').map((s: string) => s.trim()).filter(Boolean)
  await request.post(`/api/v1/products/${detail.value.id}/options`, { optionKey: newOption.key, displayName: newOption.key, values })
  newOption.key = ''; newOption.values = ''
  openDetail(detail.value.id)
}

async function removeOption(optionId: number) {
  await request.delete(`/api/v1/products/${detail.value.id}/options/${optionId}`)
  openDetail(detail.value.id)
}

async function generate() {
  const res: any = await request.post(`/api/v1/products/${detail.value.id}/variants/generate`, {})
  ElMessage.success(`生成 ${res.data.data} 个新变体`)
  openDetail(detail.value.id)
}

async function saveVariant(row: any) {
  await request.put(`/api/v1/products/variants/${row.id}`, { priceCents: row.priceCents })
  ElMessage.success('价格已保存')
}

async function saveInventory(row: any) {
  const res: any = await request.put(`/api/v1/products/variants/${row.id}/inventory`, { quantity: row.quantity })
  row.reserved = res.data.data.reserved
  row.available = res.data.data.available
  ElMessage.success('库存已保存')
}

onMounted(load)
</script>
