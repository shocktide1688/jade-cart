<template>
  <div>
    <el-card>
      <template #header>
        <span>系统配置</span>
      </template>
      <el-form
        v-loading="loading"
        :model="form"
        label-width="120"
        style="max-width: 560px"
      >
        <el-form-item label="应用标题">
          <el-input
            v-model="form['app.title']"
            placeholder="管理端左上角标题"
            maxlength="30"
            show-word-limit
          />
          <div class="muted-tip">
            显示在管理端左上角 (侧边栏 Logo 处) 与登录页品牌区, 保存后刷新页面生效。
          </div>
        </el-form-item>
        <el-form-item label="登录欢迎语">
          <el-input
            v-model="form['login.welcome']"
            placeholder="登录框上方的欢迎文案"
            maxlength="30"
            show-word-limit
          />
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SystemConfig' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const saving = ref(false)

const form = reactive<Record<string, string>>({
  'app.title': '',
  'login.welcome': '',
})

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/sys-config')
    form['app.title'] = res.data.data['app.title'] || ''
    form['login.welcome'] = res.data.data['login.welcome'] || ''
  } finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    await request.put('/api/v1/sys-config', {
      'app.title': form['app.title'].trim() || 'Jade Platform',
      'login.welcome': form['login.welcome'].trim() || '欢迎登录',
    })
    // 同步刷新布局标题与登录页欢迎语
    await appStore.loadConfig()
    ElMessage.success('已保存')
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.muted-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
  width: 100%;
}
</style>
