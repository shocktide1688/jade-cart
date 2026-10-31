<template>
  <div>
    <el-card>
      <template #header><span>邮件配置 (SMTP)</span></template>
      <el-form :model="form" label-width="110" style="max-width: 560px">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="form.host" placeholder="如 smtp.exmail.com" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="加密方式">
          <el-radio-group v-model="form.encryptType">
            <el-radio value="SSL">SSL (465)</el-radio>
            <el-radio value="STARTTLS">STARTTLS (587)</el-radio>
            <el-radio value="NONE">无 (25)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="SMTP 账号, 通常为邮箱地址" />
        </el-form-item>
        <el-form-item label="密码/授权码">
          <el-input v-model="form.password" type="password" show-password
                    :placeholder="form.passwordMasked ? '留空 = 保留已保存的密码' : 'SMTP 密码或授权码'" />
        </el-form-item>
        <el-form-item label="发件人地址">
          <el-input v-model="form.fromAddress" placeholder="如 noreply@company.com" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button :disabled="!form.enabled" @click="testVisible = true">发送测试邮件</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="testVisible" title="发送测试邮件" width="420px">
      <el-input v-model="testTo" placeholder="收件邮箱地址" />
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" :loading="testing" @click="sendTest">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const form = ref<any>({
  host: '', port: 465, username: '', password: '',
  passwordMasked: false, fromAddress: '', encryptType: 'SSL', enabled: false,
})
const saving = ref(false)
const testing = ref(false)
const testVisible = ref(false)
const testTo = ref('')

async function load() {
  const res = await request({ url: '/api/v1/mail/config', method: 'GET' })
  const c = res.data.data
  form.value = { ...c, password: '', passwordMasked: !!c.passwordMasked }
}

async function save() {
  saving.value = true
  try {
    await request({ url: '/api/v1/mail/config', method: 'PUT', data: form.value })
    ElMessage.success('已保存')
    await load()
  } finally { saving.value = false }
}

async function sendTest() {
  if (!testTo.value.trim()) return ElMessage.warning('请填写收件邮箱')
  testing.value = true
  try {
    const res = await request({ url: `/api/v1/mail/test?to=${encodeURIComponent(testTo.value)}`, method: 'POST' })
    ElMessage.success(res.data.data || '已发送')
    testVisible.value = false
  } finally { testing.value = false }
}

onMounted(load)
</script>
