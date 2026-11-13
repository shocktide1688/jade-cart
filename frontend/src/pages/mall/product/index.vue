<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>商品管理</span>
          <el-button
            type="primary"
            @click="openCreate"
          >
            新建商品
          </el-button>
        </div>
      </template>
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-input
          v-model="query.keyword"
          placeholder="标题搜索"
          style="width:220px"
          clearable
          @keyup.enter="load"
        />
        <el-select
          v-model="query.status"
          placeholder="状态"
          style="width:130px"
          clearable
        >
          <el-option
            label="草稿"
            value="DRAFT"
          />
          <el-option
            label="上架"
            value="ACTIVE"
          />
          <el-option
            label="归档"
            value="ARCHIVED"
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
          label="图"
          width="84"
          align="center"
        >
          <template #default="{ row }">
            <img
              v-if="row.mainImagePath"
              :src="'/media/' + row.mainImagePath"
              style="width:48px;height:48px;object-fit:cover;border-radius:6px;display:block;margin:0 auto"
            >
            <div
              v-else
              style="width:48px;height:48px;border-radius:6px;background:#f4f4f4;display:flex;align-items:center;justify-content:center;color:#ccc;margin:0 auto"
            >
              ✦
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="标题"
          min-width="170"
        />
        <el-table-column
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'info' : 'warning'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="起价"
          width="100"
        >
          <template #default="{ row }">
            {{ money(row.minPriceCents) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="variantCount"
          label="变体"
          width="70"
        />
        <el-table-column
          prop="totalAvailable"
          label="可售"
          width="70"
        />
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openDetail(row.id)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="归档后店面不再展示, 确定?"
              @confirm="archive(row.id)"
            >
              <template #reference>
                <el-button
                  link
                  type="danger"
                >
                  归档
                </el-button>
              </template>
            </el-popconfirm>
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

    <!-- 新建商品 -->
    <el-dialog
      v-model="createVisible"
      title="新建商品"
      width="640px"
      top="6vh"
    >
      <el-form
        :model="createForm"
        label-width="100"
      >
        <el-form-item label="商品图">
          <div style="display:flex;gap:12px;align-items:center">
            <img
              v-if="createForm.mainImagePath"
              :src="'/media/' + createForm.mainImagePath"
              style="height:80px;border-radius:6px;border:1px solid #eee"
            >
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :auto-upload="false"
              :on-change="uploadCreateImage"
              :disabled="createUploading"
            >
              <el-button :loading="createUploading">
                {{ createForm.mainImagePath ? '更换商品图' : '上传商品图' }}
              </el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item
          label="标题"
          required
        >
          <el-input
            v-model="createForm.title"
            placeholder="商品标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="createForm.categoryIds"
            multiple
            placeholder="选择分类 (可多选)"
            style="width:100%"
          >
            <el-option
              v-for="c in categories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="价格 (元)"
          required
        >
          <el-input-number
            v-model="createForm.priceYuan"
            :precision="2"
            :step="1"
            :min="0"
            style="width:180px"
          />
          <span
            class="muted"
            style="margin-left:10px;font-size:12px"
          >店面前台显示 {{ money(Math.round(createForm.priceYuan * 100)) }}</span>
        </el-form-item>
        <el-form-item label="SKU">
          <el-input
            v-model="createForm.defaultSku"
            placeholder="留空自动生成"
            style="width:240px"
          />
        </el-form-item>
        <el-form-item label="描述">
          <RichEditor
            v-model="createForm.description"
            style="width:100%"
            height="220px"
          />
        </el-form-item>
        <el-form-item label="SEO 描述">
          <el-input
            v-model="createForm.seoDescription"
            type="textarea"
            :rows="2"
            placeholder="搜索引擎优化描述, 留空取描述前 160 字"
          />
        </el-form-item>
        <el-form-item label="SEO 关键词">
          <el-input
            v-model="createForm.seoKeywords"
            placeholder="逗号分隔: jade, necklace, jewelry"
          />
        </el-form-item>
        <el-form-item label="直接上架">
          <el-switch v-model="createForm.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="create"
        >
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 商品编辑抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="detail?.title || '商品编辑'"
      size="60%"
      top="4vh"
      @opened="drawerOpened = true"
      @close="drawerOpened = false"
    >
      <template v-if="detail">
        <h4>基本信息</h4>
        <el-form label-width="90">
          <el-form-item label="标题">
            <el-input
              v-model="detail.title"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="Slug">
            <el-input
              v-model="detail.slug"
              disabled
            />
          </el-form-item>
          <el-form-item label="分类">
            <el-select
              v-model="detail.categoryIds"
              multiple
              placeholder="选择分类"
              style="width:100%"
            >
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <RichEditor
              v-if="drawerOpened"
              v-model="detail.description"
              style="width:100%"
              height="220px"
            />
            <div
              v-else
              style="height:220px;border:1px dashed #dcdfe6;border-radius:4px;width:100%"
            />
          </el-form-item>
          <el-form-item label="SEO 描述">
            <el-input
              v-model="detail.seoDescription"
              type="textarea"
              :rows="2"
            />
          </el-form-item>
          <el-form-item label="SEO 关键词">
            <el-input
              v-model="detail.seoKeywords"
              placeholder="逗号分隔: jade, necklace, jewelry"
            />
          </el-form-item>
          <el-form-item label="状态 / 精选">
            <el-select
              v-model="detail.status"
              style="width:130px"
            >
              <el-option
                label="草稿"
                value="DRAFT"
              />
              <el-option
                label="上架"
                value="ACTIVE"
              />
              <el-option
                label="归档"
                value="ARCHIVED"
              />
            </el-select>
            <el-switch
              v-model="detail.featured"
              active-text="首页精选"
              style="margin-left:16px"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="savingBasic"
              @click="saveBasic"
            >
              保存基本信息
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <h4>商品图</h4>
        <div style="display:flex;gap:12px;align-items:center;margin-bottom:8px">
          <img
            v-if="detail.mainImagePath"
            :src="'/media/' + detail.mainImagePath"
            style="width:80px;height:80px;object-fit:cover;border-radius:6px;border:1px solid #eee"
          >
          <div
            v-else
            style="width:80px;height:80px;border-radius:6px;display:flex;align-items:center;justify-content:center;color:#ccc;background:#f4f4f4"
          >
            ✦
          </div>
          <el-upload
            :show-file-list="false"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :on-change="uploadImage"
            :disabled="uploading"
          >
            <el-button :loading="uploading">
              {{ detail.mainImagePath ? '更换主图' : '上传主图' }}
            </el-button>
          </el-upload>
          <el-button
            v-if="detail.mainImagePath"
            link
            type="danger"
            @click="removeImage"
          >
            移除
          </el-button>
        </div>

        <el-divider />

        <h4>
          图集 <span
            class="muted"
            style="font-weight:400;font-size:12px"
          >店面详情页相册 (主图 + 图集, 可拖拽排序)</span>
        </h4>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom:12px;width:100%"
          title="建议上传纯产品图 (不带文字的海报)"
          description="缩略图只有 72px, 带文字的营销海报缩小后看不清, 文字会被裁切; 营销文案请放在商品描述里, 图集只放产品多角度照片。"
        />
        <div
          v-loading="galleryLoading"
          style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:10px"
        >
          <div
            v-for="(g, gi) in gallery"
            :key="g.id + '-' + gi"
            class="gal-item"
            :class="{ main: g.main }"
            draggable="true"
            @dragstart="dragIndex = gi"
            @dragover.prevent
            @drop="dropImage(gi)"
          >
            <img
              :src="'/media/' + g.path"
              :style="{ objectPosition: g.main ? 'center' : 'center' }"
              alt=""
            >
            <div class="gal-badge">
              {{ g.main ? '主图' : gi }}
            </div>
            <div
              v-if="!g.main"
              class="gal-ops"
            >
              <el-button
                link
                type="primary"
                size="small"
                @click.stop="promoteImage(g)"
              >
                设为主图
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                @click.stop="deleteImage(g)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div
            v-if="!gallery.length"
            style="color:#909399;font-size:13px;padding:8px 0"
          >
            暂无图集图片 —— 店面详情页只显示主图
          </div>
        </div>
        <el-upload
          :show-file-list="false"
          multiple
          accept="image/jpeg,image/png,image/webp"
          :auto-upload="false"
          :on-change="uploadGalleryImages"
          :disabled="galleryUploading"
        >
          <el-button :loading="galleryUploading">
            上传图集图片 (可多选)
          </el-button>
        </el-upload>

        <el-divider />

        <h4>
          变体轴 <span
            class="muted"
            style="font-size:12px;font-weight:400"
          >(如颜色/尺码, 用于生成多规格)</span>
        </h4>
        <div
          v-for="opt in detail.options"
          :key="opt.id"
          style="margin-bottom:8px;display:flex;gap:8px;align-items:center;flex-wrap:wrap"
        >
          <el-tag>{{ opt.displayName }}</el-tag>
          <el-tag
            v-for="v in opt.values"
            :key="v"
            type="info"
            size="small"
          >
            {{ v }}
          </el-tag>
          <el-button
            link
            type="danger"
            size="small"
            @click="removeOption(opt.id)"
          >
            删除轴
          </el-button>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:8px;flex-wrap:wrap">
          <el-input
            v-model="newOption.key"
            placeholder="轴名, 如 color"
            style="width:130px"
          />
          <el-input
            v-model="newOption.values"
            placeholder="取值, 逗号分隔: Red,Blue"
            style="width:220px"
          />
          <el-button @click="addOption">
            加轴
          </el-button>
          <el-button
            type="primary"
            plain
            :disabled="!detail.options.length"
            @click="generate"
          >
            生成变体矩阵
          </el-button>
        </div>

        <el-divider />

        <h4>变体 / 库存 / 价格</h4>
        <el-table
          :data="detail.variants"
          size="small"
          border
        >
          <el-table-column
            prop="sku"
            label="SKU"
            min-width="130"
          />
          <el-table-column
            prop="spec"
            label="规格"
            min-width="110"
          />
          <el-table-column
            label="价格 (元)"
            width="140"
          >
            <template #default="{ row }">
              <el-input-number
                v-model="row.priceYuan"
                :precision="2"
                :step="1"
                :min="0"
                :controls="false"
                size="small"
                style="width:110px"
                @change="saveVariant(row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="在库"
            width="90"
          >
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="0"
                :controls="false"
                size="small"
                style="width:70px"
                @change="saveInventory(row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="reserved"
            label="占用"
            width="60"
          />
          <el-table-column
            prop="available"
            label="可售"
            width="60"
          />
          <el-table-column
            label=""
            width="60"
          >
            <template #default="{ row }">
              <el-popconfirm
                title="删除该变体?"
                @confirm="deleteVariant(row.id)"
              >
                <template #reference>
                  <el-button
                    link
                    type="danger"
                    size="small"
                  >
                    删
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-button
          size="small"
          style="margin-top:8px"
          @click="addManualVariant"
        >
          手动加变体
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallProduct' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import RichEditor from '@/components/RichEditor.vue'

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
  description: '', seoDescription: '', seoKeywords: '', mainImagePath: '', active: true,
})
const detailVisible = ref(false)
const detail = ref<any>(null)
const uploading = ref(false)
// 图集 (店面详情页相册): 主图 + 补充图, 补充图可拖拽排序
const gallery = ref<any[]>([])
const galleryLoading = ref(false)
const galleryUploading = ref(false)
const dragIndex = ref(-1)
const savingBasic = ref(false)
const newOption = reactive({ key: '', values: '' })
const drawerOpened = ref(false)

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
    description: '', seoDescription: '', seoKeywords: '', mainImagePath: '', active: true,
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
      seoKeywords: createForm.seoKeywords || undefined,
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
  loadGallery(id)
}

/** 拉取图集 (主图 + 补充图) */
async function loadGallery(id?: number) {
  const productId = id ?? detail.value?.id
  if (!productId) return
  galleryLoading.value = true
  try {
    const res: any = await request.get(`/api/v1/products/${productId}/images`)
    gallery.value = res.data.data || []
  } finally { galleryLoading.value = false }
}

/** 多选上传图集图片 */
async function uploadGalleryImages(file: any) {
  galleryUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await request.post('/api/v1/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    await request.post(`/api/v1/products/${detail.value.id}/images`, { path: res.data.data.path })
    ElMessage.success('图集图片已添加')
    loadGallery()
  } finally { galleryUploading.value = false }
}

async function deleteImage(g: any) {
  await request.delete(`/api/v1/products/${detail.value.id}/images/${g.id}`)
  ElMessage.success('已删除')
  loadGallery()
}

async function promoteImage(g: any) {
  await request.put(`/api/v1/products/${detail.value.id}/images/${g.id}/main`)
  ElMessage.success('已设为主图')
  loadGallery()
}

/** 拖拽排序: 把 dragIndex 的补充图移到目标位置 */
async function dropImage(target: number) {
  const from = dragIndex.value
  dragIndex.value = -1
  if (from < 0 || from === target) return
  const list = [...gallery.value]
  const moved = list.splice(from, 1)[0]
  list.splice(target, 0, moved)
  gallery.value = list
  // 提交新顺序 (只含补充图, 主图固定首位)
  const imageIds = list.filter((g) => !g.main).map((g) => g.id)
  await request.put(`/api/v1/products/${detail.value.id}/images/order`, { imageIds })
  ElMessage.success('顺序已保存')
  loadGallery()
}

async function saveBasic() {
  savingBasic.value = true
  try {
    await request.put(`/api/v1/products/${detail.value.id}`, {
      title: detail.value.title,
      description: detail.value.description,
      seoDescription: detail.value.seoDescription,
      seoKeywords: detail.value.seoKeywords,
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

/* 图集项: 缩略图 + 主图角标 + 悬浮操作 */
.gal-item {
  position: relative;
  width: 92px;
  height: 92px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e6e6e6;
  background: #fafafa;
  cursor: grab;
}
.gal-item.main { border-color: #00a86b; box-shadow: 0 0 0 2px rgba(0, 168, 107, .18); }
.gal-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.gal-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 1px 6px;
  font-size: 11px;
  border-radius: 0 0 6px 0;
  background: rgba(0, 0, 0, .55);
  color: #fff;
}
.gal-item.main .gal-badge { background: #00a86b; }
.gal-ops {
  position: absolute;
  inset: auto 0 0 0;
  display: flex;
  justify-content: space-around;
  background: rgba(0, 0, 0, .6);
  opacity: 0;
  transition: opacity .15s;
}
.gal-item:hover .gal-ops { opacity: 1; }
.gal-ops :deep(.el-button) { color: #fff; padding: 2px 4px; }
.gal-ops :deep(.el-button--danger) { color: #ff7875; }
.gal-ops :deep(.el-button--primary) { color: #7ee2b8; }
</style>
