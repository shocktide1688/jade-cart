<template>
  <div>
    <el-card>
      <template #header><span>LDAP 认证源</span></template>
      <el-form :model="form" label-width="130" style="max-width: 620px">
        <el-form-item label="LDAP URL">
          <el-input v-model="form.ldapUrl" placeholder="如 ldap://localhost:1389 (jade-ldap 默认)" />
        </el-form-item>
        <el-form-item label="Base DN">
          <el-input v-model="form.baseDn" placeholder="如 dc=jade,dc=local" />
        </el-form-item>
        <el-form-item label="绑定 DN (搜索用)">
          <el-input v-model="form.bindDn" placeholder="如 cn=admin,dc=jade,dc=local" />
        </el-form-item>
        <el-form-item label="绑定密码">
          <el-input v-model="form.bindPassword" type="password" show-password
                    :placeholder="form.passwordMasked ? '留空 = 保留已保存的密码' : '绑定账号密码'" />
        </el-form-item>
        <el-form-item label="用户过滤器">
          <el-input v-model="form.userSearchFilter" placeholder="(uid={0}) — {0} 为登录用户名" />
        </el-form-item>
        <el-form-item label="自动创建用户">
          <el-switch v-model="form.autoCreateUser" />
          <span class="hint">首次 LDAP 认证通过后自动创建本地账号</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="hint">启用后: 本地密码校验失败时转 LDAP 认证</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button :loading="testing" @click="testConn(false)">测试连接</el-button>
          <el-button :disabled="!form.enabled || !testUser || !testPwd" :loading="testingUser"
                     @click="testConn(true)">测试用户登录</el-button>
        </el-form-item>
        <template v-if="form.enabled">
          <el-divider content-position="left">测试用户登录 (可选验证)</el-divider>
          <el-form-item label="测试用户名">
            <el-input v-model="testUser" style="max-width: 300px" />
          </el-form-item>
          <el-form-item label="测试密码">
            <el-input v-model="testPwd" type="password" show-password style="max-width: 300px" />
          </el-form-item>
        </template>
        <el-alert v-if="testResult" :type="testResult.startsWith('连接成功') ? 'success' : 'error'"
                  :closable="false" show-icon>{{ testResult }}</el-alert>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const form = ref<any>({
  ldapUrl: '', baseDn: '', bindDn: '', bindPassword: '',
  passwordMasked: false, userSearchFilter: '(uid={0})',
  autoCreateUser: true, enabled: false,
})
const saving = ref(false)
const testing = ref(false)
const testingUser = ref(false)
const testUser = ref('')
const testPwd = ref('')
const testResult = ref('')

async function load() {
  const res = await request({ url: '/api/v1/ldap/config', method: 'GET' })
  const c = res.data.data
  form.value = { ...c, bindPassword: '', passwordMasked: !!c.bindPasswordMasked }
}

async function save() {
  saving.value = true
  try {
    await request({ url: '/api/v1/ldap/config', method: 'PUT', data: form.value })
    ElMessage.success('已保存')
    await load()
  } finally { saving.value = false }
}

async function testConn(withUser: boolean) {
  testing.value = !withUser
  testingUser.value = withUser
  testResult.value = ''
  try {
    const qs = withUser
      ? `?username=${encodeURIComponent(testUser.value)}&password=${encodeURIComponent(testPwd.value)}`
      : ''
    const res = await request({ url: `/api/v1/ldap/test${qs}`, method: 'POST' })
    testResult.value = res.data.data || '未知结果'
  } finally {
    testing.value = false
    testingUser.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
</style>
