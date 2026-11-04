<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>博客管理</span>
          <el-button
            type="primary"
            @click="openCreate"
          >
            新建文章
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="rows"
        border
      >
        <el-table-column
          label="封面"
          width="90"
        >
          <template #default="{ row }">
            <img
              v-if="row.coverPath"
              :src="'/media/' + row.coverPath"
              style="width:60px;height:40px;object-fit:cover;border-radius:4px"
            >
            <div
              v-else
              style="width:60px;height:40px;border-radius:4px;background:#f4f4f4;display:flex;align-items:center;justify-content:center;color:#ccc"
            >
              ✎
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="标题"
          min-width="180"
        />
        <el-table-column
          prop="slug"
          label="Slug"
          min-width="140"
        />
        <el-table-column
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="publishedAt"
          label="发布时间"
          width="170"
        />
        <el-table-column
          label="预览"
          width="90"
        >
          <template #default="{ row }">
            <a
              v-if="row.status === 'ACTIVE'"
              :href="'http://localhost:8090/blog/' + row.slug"
              target="_blank"
            >
              <el-button
                link
                type="primary"
              >查看</el-button>
            </a>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除?"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button
                  link
                  type="danger"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 文章编辑抽屉 -->
    <el-drawer
      v-model="dialogVisible"
      :title="form.id ? '编辑文章' : '新建文章'"
      size="60%"
      top="4vh"
      :destroy-on-close="true"
      @opened="drawerOpened = true"
      @close="drawerOpened = false"
    >
      <el-form
        :model="form"
        label-width="100"
      >
        <el-form-item
          label="标题"
          required
        >
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          v-if="!form.id"
          label="Slug"
        >
          <el-input
            v-model="form.slug"
            placeholder="留空按标题生成"
          />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex;gap:12px;align-items:center">
            <img
              v-if="form.coverPath"
              :src="'/media/' + form.coverPath"
              style="height:60px;border-radius:4px;border:1px solid #eee"
            >
            <el-upload
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :auto-upload="false"
              :on-change="uploadCover"
              :disabled="uploading"
            >
              <el-button :loading="uploading">
                {{ form.coverPath ? '更换封面' : '上传封面' }}
              </el-button>
            </el-upload>
            <el-button
              v-if="form.coverPath"
              link
              type="danger"
              @click="form.coverPath = ''"
            >
              移除
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="form.excerpt"
            type="textarea"
            :rows="2"
            placeholder="列表页展示, 留空取正文"
          />
        </el-form-item>
        <el-form-item label="正文">
          <!-- wangEditor 在隐藏/动画容器中初始化会静默失败, 等 drawer 完全打开再挂载 -->
          <RichEditor
            v-if="drawerOpened"
            :key="form.id || 'new'"
            v-model="form.bodyHtml"
            style="width:100%"
            height="380px"
          />
          <div
            v-else
            style="height:380px;border:1px dashed #dcdfe6;border-radius:4px;width:100%"
          />
        </el-form-item>
        <el-form-item label="SEO 描述">
          <el-input
            v-model="form.seoDescription"
            type="textarea"
            :rows="2"
            placeholder="meta description, 留空取摘要/正文"
          />
        </el-form-item>
        <el-form-item label="SEO 关键词">
          <el-input
            v-model="form.seoKeywords"
            placeholder="逗号分隔: jade, care, guide"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">
              发布
            </el-radio>
            <el-radio value="DRAFT">
              草稿
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="saving"
            @click="save"
          >
            保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallBlog' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import RichEditor from '@/components/RichEditor.vue'

const rows = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({
  id: 0, slug: '', title: '', excerpt: '', bodyHtml: '',
  coverPath: '', seoDescription: '', seoKeywords: '', status: 'ACTIVE',
})
const drawerOpened = ref(false)

async function load() {
  document.body.dataset.mountProbe = (document.body.dataset.mountProbe || '') + 'L'
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/blog-posts')
    rows.value = res.data.data
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, {
    id: 0, slug: '', title: '', excerpt: '', bodyHtml: '',
    coverPath: '', seoDescription: '', seoKeywords: '', status: 'ACTIVE',
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id, slug: row.slug, title: row.title, excerpt: row.excerpt || '',
    bodyHtml: row.bodyHtml || row.bodyMarkdown || '', coverPath: row.coverPath || '',
    seoDescription: row.seoDescription || '', seoKeywords: row.seoKeywords || '', status: row.status,
  })
  dialogVisible.value = true
}

async function uploadCover(file: any) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await request.post('/api/v1/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    form.coverPath = res.data.data.path
    ElMessage.success('封面已上传')
  } finally { uploading.value = false }
}

async function save() {
  if (!form.title) return ElMessage.warning('请输入标题')
  saving.value = true
  try {
    if (form.id) {
      await request.put(`/api/v1/blog-posts/${form.id}`, {
        title: form.title, excerpt: form.excerpt, bodyHtml: form.bodyHtml,
        coverPath: form.coverPath, seoDescription: form.seoDescription,
        seoKeywords: form.seoKeywords, status: form.status,
      })
    } else {
      await request.post('/api/v1/blog-posts', {
        slug: form.slug || undefined, title: form.title, excerpt: form.excerpt,
        bodyHtml: form.bodyHtml, coverPath: form.coverPath,
        seoDescription: form.seoDescription, seoKeywords: form.seoKeywords, status: form.status,
      })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(id: number) {
  await request.delete(`/api/v1/blog-posts/${id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
