<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request, { BizError } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({ username: 'admin', password: 'admin123', uuid: '', code: '' })
const loading = ref(false)
// 失败超次（1005）或验证码错误（1006）后显示验证码行
const captchaVisible = ref(false)
const captchaImage = ref('')

async function refreshCaptcha() {
  const res = await request({ url: '/api/v1/auth/captcha', method: 'GET' })
  const data = res.data.data as { uuid: string; image: string }
  captchaImage.value = data.image
  form.uuid = data.uuid
}

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e) {
    // 需要验证码/验证码错误 → 显示验证码行并换图（错误提示已在 request.ts 统一弹出）
    if (e instanceof BizError && (e.code === 1005 || e.code === 1006)) {
      captchaVisible.value = true
      form.code = ''
      await refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="brand">
        <h1 class="title">
          Jade Platform
        </h1>
        <p class="subtitle">
          温润如玉 · 稳定如石
        </p>
      </div>
    </div>

    <div class="login-box">
      <h2 class="form-title">
        欢迎登录
      </h2>
      <el-form
        :model="form"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item v-if="captchaVisible" label="验证码">
          <div class="captcha-row">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              size="large"
              maxlength="4"
              @keyup.enter="handleLogin"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              title="看不清？点击刷新"
              alt="验证码"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width: 100%; background: #00a86b; border-color: #00a86b;"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
      <p class="tip">
        默认账号：admin / admin123
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
}
.login-bg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.brand { text-align: center; }
.title { font-size: 56px; margin: 0 0 12px; letter-spacing: 6px; color: #00a86b; }
.subtitle { font-size: 18px; opacity: 0.7; letter-spacing: 4px; }
.login-box {
  width: 420px;
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  margin-right: 100px;
  align-self: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}
.form-title { text-align: center; margin: 0 0 32px; color: #303133; }
.tip { text-align: center; color: #909399; font-size: 12px; margin-top: 16px; }

.captcha-row { display: flex; gap: 12px; width: 100%; }
.captcha-img { height: 40px; border-radius: 4px; cursor: pointer; flex-shrink: 0; }
</style>
