<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>内容管理</span>
          <div>
            <el-button
              type="success"
              :loading="generating"
              @click="genPolicies"
            >
              生成政策页 (7个)
            </el-button>
            <el-button
              type="primary"
              @click="openCreate"
            >
              新建页面
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="rows"
        border
      >
        <el-table-column
          prop="id"
          label="ID"
          width="70"
        />
        <el-table-column
          prop="slug"
          label="Slug"
          min-width="150"
        />
        <el-table-column
          prop="title"
          label="标题"
          min-width="160"
        />
        <el-table-column
          label="类型"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.policy ? 'warning' : 'info'">
              {{ row.policy ? '政策' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="预览"
          width="90"
        >
          <template #default="{ row }">
            <a
              :href="'http://localhost:8090/pages/' + row.slug"
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑页面' : '新建页面'"
      width="640px"
    >
      <el-form
        :model="form"
        label-width="90"
      >
        <el-form-item
          v-if="!form.id"
          label="Slug"
        >
          <el-input
            v-model="form.slug"
            placeholder="privacy-policy (小写字母/数字/连字符)"
          />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="Markdown">
          <el-input
            v-model="form.bodyMarkdown"
            type="textarea"
            :rows="12"
            placeholder="## 标题&#10;&#10;段落文字, **加粗**, *斜体*, [链接](https://...), - 列表"
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="save"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallContent' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const generating = ref(false)
const form = reactive({ id: 0, slug: '', title: '', bodyMarkdown: '', status: 'ACTIVE' })

async function load() {
  loading.value = true
  try {
    rows.value = (await request.get('/api/v1/content-pages')).data.data
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: 0, slug: '', title: '', bodyMarkdown: '', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { id: row.id, slug: row.slug, title: row.title, bodyMarkdown: row.bodyMarkdown || '', status: row.status })
  dialogVisible.value = true
}

async function save() {
  if (!form.title) return ElMessage.warning('请输入标题')
  saving.value = true
  try {
    if (form.id) {
      await request.put(`/api/v1/content-pages/${form.id}`, {
        title: form.title, bodyMarkdown: form.bodyMarkdown, status: form.status,
      })
    } else {
      await request.post('/api/v1/content-pages', {
        slug: form.slug, title: form.title, bodyMarkdown: form.bodyMarkdown,
      })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(id: number) {
  await request.delete(`/api/v1/content-pages/${id}`)
  ElMessage.success('已删除')
  load()
}

async function genPolicies() {
  generating.value = true
  try {
    const res: any = await request.post('/api/v1/content-pages/generate-policies')
    ElMessage.success(`已生成/更新 ${res.data.data} 个政策页`)
    load()
  } finally { generating.value = false }
}

onMounted(load)
</script>
