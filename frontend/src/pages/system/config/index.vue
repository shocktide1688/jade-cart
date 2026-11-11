<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-head">
          <span>参数设置</span>
          <el-button
            type="primary"
            size="small"
            @click="openCreate"
          >
            新增参数
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="rows"
        border
        stripe
      >
        <el-table-column
          prop="configName"
          label="名称"
          min-width="140"
        />
        <el-table-column
          prop="configKey"
          label="键"
          min-width="160"
        />
        <el-table-column
          prop="configValue"
          label="值"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="configType"
          label="类型"
          width="90"
        />
        <el-table-column
          label="内置"
          width="70"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.isBuiltin === 1 ? 'warning' : 'info'"
              size="small"
            >
              {{ row.isBuiltin === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          min-width="160"
          show-overflow-tooltip
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
              :disabled="row.isBuiltin === 1"
              title="系统内置参数不可修改"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.isBuiltin === 1"
              title="系统内置参数不可删除"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑参数' : '新增参数'"
      width="520"
    >
      <el-form
        :model="form"
        label-width="90"
      >
        <el-form-item label="名称">
          <el-input
            v-model="form.configName"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="键">
          <el-input
            v-model="form.configKey"
            :disabled="!!editingId"
            placeholder="如 app.title"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="值">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
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
defineOptions({ name: 'SystemConfig' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

interface ConfigRow {
  id: number
  configName: string
  configKey: string
  configValue: string | null
  configType: string | null
  isBuiltin: number
  remark: string | null
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<ConfigRow[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({ configName: '', configKey: '', configValue: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/configs/all')
    rows.value = res.data.data || []
  } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { configName: '', configKey: '', configValue: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, {
    configName: row.configName, configKey: row.configKey,
    configValue: row.configValue || '', remark: row.remark || '',
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.configName.trim() || !form.configKey.trim()) {
    ElMessage.warning('名称与键必填')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/api/v1/configs/${editingId.value}`, {
        configName: form.configName.trim(),
        configValue: form.configValue,
        remark: form.remark,
      })
      ElMessage.success('已保存')
    } else {
      await request.post('/api/v1/configs', {
        configName: form.configName.trim(),
        configKey: form.configKey.trim(),
        configValue: form.configValue,
        remark: form.remark,
      })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除参数「${row.configName}」？`, '提示', { type: 'warning' })
  await request.delete(`/api/v1/configs/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
