<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>运费设置</span>
          <el-button type="primary" @click="openCreate">新建分区</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
                title="匹配规则" description="结账按收货国家匹配: 具体国家命中优先, * 为兜底; 满额免邮门槛>0 且小计≥门槛时免运费。" />
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="countries" label="国家" min-width="120" />
        <el-table-column label="运费" width="100">
          <template #default="{ row }">{{ money(row.rateCents) }}</template>
        </el-table-column>
        <el-table-column label="满额免邮" width="110">
          <template #default="{ row }">{{ row.freeThresholdCents > 0 ? '≥ ' + money(row.freeThresholdCents) : '-' }}</template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="remove(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分区' : '新建分区'" width="460px">
      <el-form :model="form" label-width="110">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 US Standard" />
        </el-form-item>
        <el-form-item label="国家列表">
          <el-input v-model="form.countries" placeholder="US,CA 或 * (兜底)" />
        </el-form-item>
        <el-form-item label="运费(分)">
          <el-input-number v-model="form.rateCents" :min="0" />
        </el-form-item>
        <el-form-item label="满额免邮(分)">
          <el-input-number v-model="form.freeThresholdCents" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.active" />
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
const form = reactive({ id: 0, name: '', countries: '*', rateCents: 0, freeThresholdCents: 0, active: true })

const money = (c: number) => '$' + (c / 100).toFixed(2)

async function load() {
  loading.value = true
  try {
    rows.value = (await request.get('/api/v1/shipping-zones')).data.data
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: 0, name: '', countries: '*', rateCents: 0, freeThresholdCents: 0, active: true })
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请输入名称')
  saving.value = true
  try {
    const body = { name: form.name, countries: form.countries, rateCents: form.rateCents,
                   freeThresholdCents: form.freeThresholdCents, active: form.active }
    if (form.id) await request.put(`/api/v1/shipping-zones/${form.id}`, body)
    else await request.post('/api/v1/shipping-zones', body)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function remove(id: number) {
  await request.delete(`/api/v1/shipping-zones/${id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
