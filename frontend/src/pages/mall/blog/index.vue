<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>博客管理</span>
          <el-button type="primary" @click="openCreate">新建文章</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="slug" label="Slug" min-width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170" />
        <el-table-column label="预览" width="90">
          <template #default="{ row }">
            <a v-if="row.status === 'ACTIVE'" :href="'http://localhost:8090/blog/' + row.slug" target="_blank">
              <el-button link type="primary">查看</el-button>
            </a>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="remove(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑文章' : '新建文章'" width="680px">
      <el-form :model="form" label-width="100">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="Slug" v-if="!form.id">
          <el-input v-model="form.slug" placeholder="留空按标题生成" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.excerpt" type="textarea" :rows="2" placeholder="列表页展示, 留空取正文" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex;gap:12px;align-items:center">
            <img v-if="form.coverPath" :src="'/media/' + form.coverPath"
                 style="width:80px;height:60px;object-fit:cover;border-radius:4px" />
            <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp"
                       :auto-upload="false" :on-change="uploadCover">
              <el-button :loading="uploading">{{ form.coverPath ? '更换封面' : '上传封面' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="正文 (Markdown)">
          <el-input v-model="form.bodyMarkdown" type="textarea" :rows="14"
                    placeholder="## 标题&#10;&#10;段落, **加粗**, [链接](https://...), - 列表" />
        </el-form-item>
        <el-form-item label="SEO 描述">
          <el-input v-model="form.seoDescription" type="textarea" :rows="2"
                    placeholder="meta description, 留空取摘要/正文" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">发布</el-radio>
            <el-radio value="DRAFT">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({
  id: 0, slug: '', title: '', excerpt: '', bodyMarkdown: '',
  coverPath: '', seoDescription: '', status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    rows.value = (await request.get('/api/v1/blog-posts')).data.data
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: 0, slug: '', title: '', excerpt: '', bodyMarkdown: '', coverPath: '', seoDescription: '', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, {
    id: row.id, slug: row.slug, title: row.title, excerpt: row.excerpt || '',
    bodyMarkdown: row.bodyMarkdown || '', coverPath: row.coverPath || '',
    seoDescription: row.seoDescription || '', status: row.status,
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
        title: form.title, excerpt: form.excerpt, bodyMarkdown: form.bodyMarkdown,
        coverPath: form.coverPath, seoDescription: form.seoDescription, status: form.status,
      })
    } else {
      await request.post('/api/v1/blog-posts', {
        slug: form.slug || undefined, title: form.title, excerpt: form.excerpt,
        bodyMarkdown: form.bodyMarkdown, coverPath: form.coverPath,
        seoDescription: form.seoDescription, status: form.status,
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
