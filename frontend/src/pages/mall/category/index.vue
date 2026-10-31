<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>分类管理</span>
          <el-button
            type="primary"
            @click="openCreate"
          >
            新建分类
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="rows"
        border
        row-key="id"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="70"
        />
        <el-table-column
          prop="name"
          label="名称"
          min-width="160"
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
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="sortOrder"
          label="排序"
          width="80"
        />
        <el-table-column
          label="操作"
          width="140"
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
      :title="form.id ? '编辑分类' : '新建分类'"
      width="440px"
    >
      <el-form
        :model="form"
        label-width="90"
      >
        <el-form-item
          label="名称"
          required
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Slug">
          <el-input
            v-model="form.slug"
            placeholder="留空自动生成"
          />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select
            v-model="form.parentId"
            clearable
            placeholder="无"
            style="width:100%"
          >
            <el-option
              v-for="c in rows.filter((x: any) => x.id !== form.id)"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: 0, name: '', slug: '', parentId: null as number | null, sortOrder: 0, enabled: true })

async function load() {
  loading.value = true
  try {
    rows.value = (await request.get('/api/v1/categories')).data.data
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: 0, name: '', slug: '', parentId: null, sortOrder: 0, enabled: true })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { id: row.id, name: row.name, slug: row.slug, parentId: row.parentId, sortOrder: row.sortOrder, enabled: row.status === 1 })
  dialogVisible.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入名称')
  saving.value = true
  try {
    const body = { name: form.name, slug: form.slug || undefined, parentId: form.parentId, sortOrder: form.sortOrder, status: form.enabled ? 1 : 0 }
    if (form.id) await request.put(`/categories/${form.id}`, body)
    else await request.post('/api/v1/categories', body)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(id: number) {
  await request.delete(`/categories/${id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
