<template>
  <div>
    <el-row :gutter="16">
      <!-- 左: 区块编排 -->
      <el-col :span="11">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>首页区块编排</span>
              <el-select
                v-model="addType"
                placeholder="添加区块"
                style="width:160px"
                @change="addSection"
              >
                <el-option
                  v-for="t in typeLabels"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </el-select>
            </div>
          </template>
          <el-empty
            v-if="sections.length === 0"
            description="暂无区块 — 添加后首页按此顺序渲染 (空则回退内置布局)"
          />
          <transition-group name="list">
            <div
              v-for="(s, i) in sections"
              :key="i"
              style="display:flex;align-items:center;gap:8px;border:1px solid #e6e6e6;border-radius:8px;padding:10px 12px;margin-bottom:8px"
            >
              <span style="cursor:grab;color:#999">≡</span>
              <strong style="min-width:90px">{{ typeLabels.find(t => t.value === s.type)?.label }}</strong>
              <span
                class="muted"
                style="flex:1;font-size:12px"
              >{{ summary(s) }}</span>
              <el-switch
                v-model="s.visible"
                inline-prompt
                active-text="显"
                inactive-text="隐"
                size="small"
              />
              <el-button
                link
                :disabled="i === 0"
                @click="move(i, -1)"
              >
                ↑
              </el-button>
              <el-button
                link
                :disabled="i === sections.length - 1"
                @click="move(i, 1)"
              >
                ↓
              </el-button>
              <el-button
                link
                type="primary"
                @click="editIndex = i"
              >
                配置
              </el-button>
              <el-popconfirm
                title="删除该区块?"
                @confirm="sections.splice(i, 1)"
              >
                <template #reference>
                  <el-button
                    link
                    type="danger"
                  >
                    删
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </transition-group>
          <div style="margin-top:16px;display:flex;gap:8px">
            <el-button
              type="primary"
              :loading="saving"
              @click="save"
            >
              保存编排
            </el-button>
            <el-button @click="previewKey++">
              刷新预览
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右: 预览 + 配置 -->
      <el-col :span="13">
        <el-card v-if="editing">
          <template #header>
            <span>配置: {{ typeLabels.find(t => t.value === editing.type)?.label }}</span>
          </template>
          <el-form label-width="90">
            <!-- hero -->
            <template v-if="editing.type === 'hero'">
              <el-form-item label="标题">
                <el-input v-model="editing.config.headline" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="editing.config.sub" />
              </el-form-item>
              <el-form-item label="背景图">
                <div style="display:flex;gap:8px;align-items:center">
                  <img
                    v-if="editing.config.image"
                    :src="'/media/' + editing.config.image"
                    style="height:40px;border-radius:4px"
                  >
                  <el-upload
                    :show-file-list="false"
                    accept="image/*"
                    :auto-upload="false"
                    :on-change="uploadHeroImage"
                  >
                    <el-button size="small">
                      上传
                    </el-button>
                  </el-upload>
                </div>
              </el-form-item>
              <el-form-item label="按钮文字">
                <el-input
                  v-model="editing.config.ctaText"
                  placeholder="Shop now"
                />
              </el-form-item>
              <el-form-item label="按钮链接">
                <el-input
                  v-model="editing.config.ctaHref"
                  placeholder="/products"
                />
              </el-form-item>
              <el-form-item label="对齐">
                <el-radio-group v-model="editing.config.align">
                  <el-radio value="center">
                    居中
                  </el-radio>
                  <el-radio value="left">
                    居左
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </template>

            <!-- featured -->
            <template v-if="editing.type === 'featured'">
              <el-form-item label="标题">
                <el-input
                  v-model="editing.config.title"
                  placeholder="Featured"
                />
              </el-form-item>
              <el-form-item label="商品来源">
                <el-radio-group v-model="editing.config.source">
                  <el-radio value="featured">
                    首页精选
                  </el-radio>
                  <el-radio value="latest">
                    最新上架
                  </el-radio>
                  <el-radio value="category:">
                    按分类
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item
                v-if="(editing.config.source || '').startsWith('category')"
                label="分类 slug"
              >
                <el-input
                  v-model="editing.config.source"
                  :placeholder="editing.config.source === 'category:' ? '如 category:apparel' : ''"
                  :disabled="!editing.config.source.startsWith('category:')"
                />
              </el-form-item>
              <el-form-item label="数量">
                <el-input-number
                  v-model="editing.config.limit"
                  :min="1"
                  :max="24"
                />
              </el-form-item>
            </template>

            <!-- category_grid / blog_latest -->
            <template v-if="['category_grid', 'blog_latest'].includes(editing.type)">
              <el-form-item label="标题">
                <el-input v-model="editing.config.title" />
              </el-form-item>
              <el-form-item label="数量">
                <el-input-number
                  v-model="editing.config.limit"
                  :min="1"
                  :max="12"
                />
              </el-form-item>
            </template>

            <!-- usp -->
            <template v-if="editing.type === 'usp'">
              <div
                v-for="(item, i) in editing.config.items"
                :key="i"
                style="display:flex;gap:8px;margin-bottom:8px"
              >
                <el-input
                  v-model="item.icon"
                  style="width:70px"
                  placeholder="🚚"
                />
                <el-input
                  v-model="item.title"
                  style="width:140px"
                  placeholder="标题"
                />
                <el-input
                  v-model="item.text"
                  placeholder="说明"
                />
                <el-button
                  link
                  type="danger"
                  @click="editing.config.items.splice(i, 1)"
                >
                  删
                </el-button>
              </div>
              <el-button
                size="small"
                @click="editing.config.items.push({ icon: '✦', title: '', text: '' })"
              >
                加一条
              </el-button>
            </template>

            <!-- rich_text -->
            <template v-if="editing.type === 'rich_text'">
              <el-form-item label="标题">
                <el-input v-model="editing.config.title" />
              </el-form-item>
              <el-form-item label="Markdown">
                <el-input
                  v-model="editing.config.markdown"
                  type="textarea"
                  :rows="8"
                />
              </el-form-item>
            </template>

            <!-- banner_strip -->
            <template v-if="editing.type === 'banner_strip'">
              <el-form-item label="文案">
                <el-input
                  v-model="editing.config.text"
                  placeholder="Summer Sale — 20% off"
                />
              </el-form-item>
              <el-form-item label="背景色">
                <el-color-picker v-model="editing.config.bg" /><span
                  class="muted"
                  style="margin-left:8px;font-size:12px"
                >留空 = 主题主色</span>
              </el-form-item>
              <el-form-item label="链接">
                <el-input
                  v-model="editing.config.href"
                  placeholder="可选"
                />
              </el-form-item>
            </template>
          </el-form>
        </el-card>

        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>店面预览</span>
              <el-radio-group
                v-model="previewWidth"
                size="small"
              >
                <el-radio-button value="100%">
                  Desktop
                </el-radio-button>
                <el-radio-button value="390px">
                  Mobile
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <iframe
            :key="previewKey"
            :src="'http://localhost:8090/'"
            :style="{ width: previewWidth, height: '560px', border: '1px solid #ddd', borderRadius: '8px', margin: '0 auto', display: 'block' }"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallStorefront' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const sections = ref<any[]>([])
const types = ref<string[]>([])
const addType = ref('')
const editIndex = ref(-1)
const saving = ref(false)
const previewKey = ref(0)
const previewWidth = ref('100%')

const typeLabels = [
  { value: 'hero', label: 'Hero 首屏' },
  { value: 'featured', label: '商品网格' },
  { value: 'category_grid', label: '分类导航' },
  { value: 'usp', label: '卖点条' },
  { value: 'rich_text', label: '富文本' },
  { value: 'blog_latest', label: '最新文章' },
  { value: 'banner_strip', label: '公告条' },
]

const editing = ref<any>(null)

function blankFor(type: string) {
  const base: any = { visible: true }
  if (type === 'hero') Object.assign(base, { headline: '', sub: '', image: '', ctaText: 'Shop now', ctaHref: '/products', align: 'center' })
  if (type === 'featured') Object.assign(base, { title: 'Featured', source: 'featured', limit: 8 })
  if (type === 'category_grid') Object.assign(base, { title: 'Shop by Category', limit: 6 })
  if (type === 'usp') Object.assign(base, { items: [{ icon: '🚚', title: 'Fast shipping', text: 'Worldwide' }] })
  if (type === 'rich_text') Object.assign(base, { title: '', markdown: '' })
  if (type === 'blog_latest') Object.assign(base, { title: 'From the Blog', limit: 3 })
  if (type === 'banner_strip') Object.assign(base, { text: '', bg: '', href: '' })
  return base
}

function summary(s: any) {
  const c = s.config || {}
  if (s.type === 'hero') return c.headline || ''
  if (s.type === 'banner_strip') return c.text || ''
  if (s.type === 'featured') return `${c.title || ''} (${c.source || 'featured'})`
  if (s.type === 'usp') return `${(c.items || []).length} 项`
  return c.title || ''
}

async function load() {
  const res: any = await request.get('/api/v1/storefront')
  types.value = res.data.data.types
  sections.value = res.data.data.sections.map((s: any) => ({ ...s, config: s.config || {} }))
  if (editIndex.value >= sections.value.length) editIndex.value = -1
}

function addSection(type: string) {
  if (!type) return
  sections.value.push({ type, config: blankFor(type), visible: true })
  editIndex.value = sections.value.length - 1
  addType.value = ''
}

function editGuard() {
  if (editIndex.value >= 0) editing.value = sections.value[editIndex.value]
}

function move(i: number, dir: number) {
  const j = i + dir
  const arr = sections.value
  ;[arr[i], arr[j]] = [arr[j], arr[i]]
  editGuard()
}

import { watch } from 'vue'
watch(editIndex, editGuard)

async function uploadHeroImage(file: any) {
  const fd = new FormData()
  fd.append('file', file.raw)
  const res: any = await request.post('/api/v1/media/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  editing.value.config.image = res.data.data.path
}

async function save() {
  saving.value = true
  try {
    await request.put('/api/v1/storefront', sections.value.map((s: any) => ({
      type: s.type, config: s.config, visible: s.visible,
    })))
    ElMessage.success('已保存, 店面首页即时生效')
    previewKey.value++
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.list-enter-active, .list-leave-active { transition: all .2s; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-8px); }
.muted { color: #999; }
</style>
