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
        <el-table-column label="图" width="72">
          <template #default="{ row }">
            <img v-if="row.mainImagePath" :src="'/media/' + row.mainImagePath"
                 style="width:48px;height:48px;object-fit:cover;border-radius:6px" />
            <div v-else style="width:48px;height:48px;border-radius:6px;background:#f4f4f4;display:flex;align-items:center;justify-content:center;color:#ccc">✦</div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="170" />
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
        <el-table-column prop="variantCount" label="变体" width="70" />
        <el-table-column prop="totalAvailable" label="可售" width="70" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">编辑</el-button>
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
    <el-dialog v-model="createVisible" title="新建商品" width="640px" top="6vh">
      <el-form :model="createForm" label-width="100">
        <el-form-item label="商品图">
          <div style="display:flex;gap:12px;align-items:center">
            <img v-if="createForm.mainImagePath" :src="'/media/' + createForm.mainImagePath"
                 style="height:80px;border-radius:6px;border:1px solid #eee" />
            <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp" :auto-upload="false"
                       :on-change="uploadCreateImage" :disabled="createUploading">
              <el-button :loading="createUploading">{{ createForm.mainImagePath ? '更换商品图' : '上传商品图' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="商品标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.categoryIds" multiple placeholder="选择分类 (可多选)" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格 (元)" required>
          <el-input-number v-model="createForm.priceYuan" :precision="2" :step="1" :min="0" style="width:180px" />
          <span class="muted" style="margin-left:10px;font-size:12px">店面前台显示 {{ money(Math.round(createForm.priceYuan * 100)) }}</span>
        </el-form-item>
        <el-form-item label="SKU">
          <el-input v-model="createForm.defaultSku" placeholder="留空自动生成" style="width:240px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="商品描述, 显示在店面详情页" />
        </el-form-item>
        <el-form-item label="SEO 描述">
          <el-input v-model="createForm.seoDescription" type="textarea" :rows="2"
                    placeholder="搜索引擎优化描述, 留空取描述前 160 字" />
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

    <!-- 商品编辑抽屉 -->
    <el-drawer v-model="detailVisible" :title="detail?.title || '商品编辑'" size="680px" top="4vh">
      <template v-if="detail">
        <h4>基本信息</h4>
        <el-form label-width="90">
          <el-form-item label="标题">
            <el-input v-model="detail.title" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="Slug">
            <el-input v-model="detail.slug" disabled />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="detail.categoryIds" multiple placeholder="选择分类" style="width:100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="detail.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="SEO 描述">
            <el-input v-model="detail.seoDescription" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="状态 / 精选">
            <el-select v-model="detail.status" style="width:130px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="上架" value="ACTIVE" />
              <el-option label="归档" value="ARCHIVED" />
            </el-select>
            <el-switch v-model="detail.featured" active-text="首页精选" style="margin-left:16px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingBasic" @click="saveBasic">保存基本信息</el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <h4>商品图</h4>
        <div style="display:flex;gap:12px;align-items:center;margin-bottom:8px">
          <img v-if="detail.mainImagePath" :src="'/media/' + detail.mainImagePath"
               style="width:80px;height:80px;object-fit:cover;border-radius:6px;border:1px solid #eee" />
          <div v-else style="width:80px;height:80px;border-radius:6px;display:flex;align-items:center;justify-content:center;color:#ccc;background:#f4f4f4">✦</div>
          <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp"
                     :auto-upload="false" :on-change="uploadImage" :disabled="uploading">
            <el-button :loading="uploading">{{ detail.mainImagePath ? '更换主图' : '上传主图' }}</el-button>
          </el-upload>
          <el-button v-if="detail.mainImagePath" link type="danger" @click="removeImage">移除</el-button>
        </div>

        <el-divider />

        <h4>变体轴 <span class="muted" style="font-size:12px;font-weight:400">(如颜色/尺码, 用于生成多规格)</span></h4>
        <div v-for="opt in detail.options" :key="opt.id" style="margin-bottom:8px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">
          <el-tag>{{ opt.displayName }}</el-tag>
          <el-tag v-for="v in opt.values" :key="v" type="info" size="small">{{ v }}</el-tag>
          <el-button link type="danger" size="small" @click="removeOption(opt.id)">删除轴</el-button>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:8px;flex-wrap:wrap">
          <el-input v-model="newOption.key" placeholder="轴名, 如 color" style="width:130px" />
          <el-input v-model="newOption.values" placeholder="取值, 逗号分隔: Red,Blue" style="width:220px" />
          <el-button @click="addOption">加轴</el-button>
          <el-button type="primary" plain :disabled="!detail.options.length" @click="generate">生成变体矩阵</el-button>
        </div>

        <el-divider />

        <h4>变体 / 库存 / 价格</h4>
        <el-table :data="detail.variants" size="small" border>
          <el-table-column prop="sku" label="SKU" min-width="130" />
          <el-table-column prop="spec" label="规格" min-width="110" />
          <el-table-column label="价格 (元)" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.priceYuan" :precision="2" :step="1" :min="0"
                               :controls="false" size="small" style="width:110px"
                               @change="saveVariant(row)" />
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
          <el-table-column label="" width="60">
            <template #default="{ row }">
              <el-popconfirm title="删除该变体?" @confirm="deleteVariant(row.id)">
                <template #reference><el-button link type="danger" size="small">删</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" style="margin-top:8px" @click="addManualVariant">手动加变体</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const categories = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const createVisible = ref(false)
const saving = ref(false)
const createUploading = ref(false)
const createForm = reactive({
  title: '', categoryIds: [] as number[], priceYuan: 0, defaultSku: '',
  description: '', seoDescription: '', mainImagePath: '', active: true,
})
const detailVisible = ref(false)
const detail = ref<any>(null)
const uploading = ref(false)
const savingBasic = ref(false)
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

async function loadCategories() {
  categories.value = (await request.get('/api/v1/categories')).data.data
}

function openCreate() {
  Object.assign(createForm, {
    title: '', categoryIds: [], priceYuan: 0, defaultSku: '',
    description: '', seoDescription: '', mainImagePath: '', active: true,
  })
  createVisible.value = true
}

async function uploadCreateImage(file: any) {
  createUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await request.post('/api/v1/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    createForm.mainImagePath = res.data.data.path
  } finally { createUploading.value = false }
}

async function create() {
  if (!createForm.title) return ElMessage.warning('请输入标题')
  saving.value = true
  try {
    const res: any = await request.post('/api/v1/products', {
      title: createForm.title,
      description: createForm.description || undefined,
      seoDescription: createForm.seoDescription || undefined,
      defaultPriceCents: Math.round(createForm.priceYuan * 100),
      defaultSku: createForm.defaultSku || undefined,
      mainImagePath: createForm.mainImagePath || undefined,
      status: createForm.active ? 'ACTIVE' : 'DRAFT',
    })
    const id = res.data.data.id
    if (createForm.categoryIds.length) {
      await request.put(`/api/v1/products/${id}/categories`, { categoryIds: createForm.categoryIds })
    }
    ElMessage.success('商品已创建')
    createVisible.value = false
    load()
  } finally { saving.value = false }
}

async function openDetail(id: number) {
  const res: any = await request.get(`/api/v1/products/${id}`)
  detail.value = res.data.data
  detail.value.variants.forEach((v: any) => { v.priceYuan = v.priceCents / 100 })
  detailVisible.value = true
}

async function saveBasic() {
  savingBasic.value = true
  try {
    await request.put(`/api/v1/products/${detail.value.id}`, {
      title: detail.value.title,
      description: detail.value.description,
      seoDescription: detail.value.seoDescription,
      status: detail.value.status,
      featured: detail.value.featured,
    })
    await request.put(`/api/v1/products/${detail.value.id}/categories`, {
      categoryIds: detail.value.categoryIds || [],
    })
    ElMessage.success('基本信息已保存')
    load()
  } finally { savingBasic.value = false }
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
    await request.put(`/api/v1/products/${detail.value.id}`, { mainImagePath: res.data.data.path })
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
  if (!newOption.key) return ElMessage.warning('请输入轴名')
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

async function addManualVariant() {
  await request.post(`/api/v1/products/${detail.value.id}/variants`, { priceCents: 0 })
  openDetail(detail.value.id)
}

async function deleteVariant(variantId: number) {
  await request.delete(`/api/v1/products/variants/${variantId}`)
  ElMessage.success('变体已删除')
  openDetail(detail.value.id)
}

async function saveVariant(row: any) {
  await request.put(`/api/v1/products/variants/${row.id}`, { priceCents: Math.round(row.priceYuan * 100) })
  ElMessage.success('价格已保存')
}

async function saveInventory(row: any) {
  const res: any = await request.put(`/api/v1/products/variants/${row.id}/inventory`, { quantity: row.quantity })
  row.reserved = res.data.data.reserved
  row.available = res.data.data.available
  ElMessage.success('库存已保存')
}

onMounted(() => { load(); loadCategories() })
</script>

<style scoped>
.muted { color: #999; }
h4 { margin: 4px 0 10px; }
</style>
