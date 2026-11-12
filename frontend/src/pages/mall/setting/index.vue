<template>
  <div>
    <el-card>
      <template #header>
        <span>商城设置</span>
      </template>
      <el-tabs v-model="tab">
        <!-- 店铺信息 -->
        <el-tab-pane
          label="店铺信息"
          name="shop"
        >
          <el-form
            v-loading="loading"
            :model="form"
            label-width="120"
            style="max-width: 560px"
          >
            <el-form-item label="店铺名称">
              <el-input v-model="form.store_name" />
            </el-form-item>
            <el-form-item label="货币">
              <el-select
                v-model="form.currency"
                style="width:160px"
              >
                <el-option
                  label="USD 美元"
                  value="USD"
                />
                <el-option
                  label="EUR 欧元"
                  value="EUR"
                />
                <el-option
                  label="CNY 人民币"
                  value="CNY"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="客服邮箱">
              <el-input
                v-model="form.support_email"
                placeholder="support@example.com"
              />
            </el-form-item>
            <el-form-item label="店面地址">
              <el-input
                v-model="form.storefront_url"
                placeholder="https://shop.example.com"
              />
              <div class="muted-tip">
                店面首页完整地址, 用于"店面主题"页签的实时预览与结算回跳; 生产环境必填, 留空预览回落本地 8090。
              </div>
            </el-form-item>
            <el-form-item label="店面公告">
              <el-input
                v-model="form.announcement"
                type="textarea"
                :rows="3"
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
        </el-tab-pane>

        <!-- 支付设置 -->
        <el-tab-pane
          label="支付设置"
          name="payment"
        >
          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom:16px"
            title="渠道选择逻辑"
            description="结账按 Stripe → PayPal → Mock 顺序选首个已配置密钥的渠道；全部未配置时走内置 Mock 收银台。"
          />
          <el-form
            v-loading="loading"
            label-width="160"
            style="max-width: 640px"
          >
            <h4>Stripe</h4>
            <el-form-item label="Secret Key">
              <el-input
                v-model="pay.stripe_secret_key"
                type="password"
                show-password
                :placeholder="pay.stripeConfigured ? '已配置 (输入新值覆盖)' : 'sk_test_... / sk_live_...'"
              />
            </el-form-item>
            <el-form-item label="Webhook Secret">
              <el-input
                v-model="pay.stripe_webhook_secret"
                type="password"
                show-password
                :placeholder="pay.stripeWebhookConfigured ? '已配置 (输入新值覆盖)' : 'whsec_... (验签用)'"
              />
            </el-form-item>
            <el-form-item label="Webhook URL">
              <el-input
                :model-value="(pay.store_base_url || 'http://localhost:8090') + '/api/store/webhooks/stripe'"
                disabled
              />
            </el-form-item>

            <h4 style="margin-top:20px">
              PayPal
            </h4>
            <el-form-item label="Client ID">
              <el-input v-model="pay.paypal_client_id" />
            </el-form-item>
            <el-form-item label="Client Secret">
              <el-input
                v-model="pay.paypal_client_secret"
                type="password"
                show-password
                :placeholder="pay.paypalSecretConfigured ? '已配置 (输入新值覆盖)' : ''"
              />
            </el-form-item>
            <el-form-item label="环境">
              <el-radio-group v-model="pay.paypal_env">
                <el-radio value="sandbox">
                  Sandbox
                </el-radio>
                <el-radio value="live">
                  Live
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Webhook ID">
              <el-input
                v-model="pay.paypal_webhook_id"
                placeholder="验签用"
              />
            </el-form-item>

            <h4 style="margin-top:20px">
              结算回跳
            </h4>
            <el-form-item label="店面地址">
              <el-input
                v-model="pay.store_base_url"
                placeholder="http://localhost:8090"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="saving"
                @click="savePayment"
              >
                保存支付设置
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="安全"
            description="所有密钥 AES-256-GCM 加密入库, 界面不回显明文。"
          />
        </el-tab-pane>

        <!-- 店面主题 -->
        <el-tab-pane
          label="店面主题"
          name="theme"
        >
          <el-form
            v-loading="themeLoading"
            label-width="120"
            style="max-width: 720px"
          >
            <el-form-item label="预设主题">
              <div style="display:flex;gap:12px;flex-wrap:wrap">
                <div
                  v-for="p in themeInfo.presets"
                  :key="p.name"
                  :style="{ border: themeForm.preset === p.name ? '2px solid #409eff' : '1px solid #ddd',
                            borderRadius: '8px', padding: '10px 14px', cursor: 'pointer', minWidth: '110px' }"
                  @click="selectPreset(p)"
                >
                  <div style="font-weight:600;text-transform:capitalize">
                    {{ p.name }}
                  </div>
                  <div style="display:flex;gap:4px;margin-top:6px">
                    <span :style="{ width: '18px', height: '18px', borderRadius: '4px', background: p.primary, display: 'inline-block' }" />
                    <span :style="{ width: '18px', height: '18px', borderRadius: '4px', background: p.bg, border: '1px solid #eee', display: 'inline-block' }" />
                    <span :style="{ width: '18px', height: '18px', borderRadius: '4px', background: p.ink, display: 'inline-block' }" />
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="主色覆盖">
              <el-color-picker
                v-model="themeForm.primary"
                show-alpha
              />
              <span
                class="muted"
                style="margin-left:10px;font-size:12px"
              >留空 = 跟随预设</span>
            </el-form-item>
            <el-form-item label="背景色覆盖">
              <el-color-picker
                v-model="themeForm.bg"
                show-alpha
              />
            </el-form-item>
            <el-form-item label="圆角">
              <el-slider
                v-model="themeForm.radiusNum"
                :min="0"
                :max="24"
                style="max-width:280px"
              />
            </el-form-item>
            <el-form-item label="字体">
              <el-radio-group v-model="themeForm.font">
                <el-radio value="sans">
                  无衬线
                </el-radio>
                <el-radio value="serif">
                  衬线
                </el-radio>
                <el-radio value="mono">
                  等宽
                </el-radio>
                <el-radio value="jost">
                  Jost (几何无衬线)
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Logo">
              <div style="display:flex;gap:12px;align-items:center">
                <img
                  v-if="themeForm.logoPath"
                  :src="'/media/' + themeForm.logoPath"
                  style="height:30px;border:1px solid #eee;border-radius:4px"
                >
                <el-upload
                  :show-file-list="false"
                  accept="image/png,image/jpeg,image/webp,image/svg+xml"
                  :auto-upload="false"
                  :on-change="uploadLogo"
                >
                  <el-button :loading="logoUploading">
                    {{ themeForm.logoPath ? '更换' : '上传' }}
                  </el-button>
                </el-upload>
                <el-button
                  v-if="themeForm.logoPath"
                  link
                  type="danger"
                  @click="themeForm.logoPath = ''"
                >
                  移除
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="themeSaving"
                @click="saveTheme"
              >
                保存主题 (店面即时生效)
              </el-button>
            </el-form-item>
            <el-form-item label="实时预览">
              <iframe
                :key="previewKey"
                :src="storefrontUrl"
                style="width:100%;height:420px;border:1px solid #ddd;border-radius:8px"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallSetting' })
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tab = ref('shop')
const loading = ref(false)
const saving = ref(false)

const form = reactive<Record<string, string>>({
  store_name: '', currency: 'USD', support_email: '', announcement: '',
  storefront_url: '',
})
const pay = reactive<Record<string, any>>({
  stripe_secret_key: '', stripe_webhook_secret: '',
  paypal_client_id: '', paypal_client_secret: '', paypal_env: 'sandbox',
  paypal_webhook_id: '', store_base_url: 'http://localhost:8090',
  stripeConfigured: false, stripeWebhookConfigured: false, paypalSecretConfigured: false,
})

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/store-settings')
    const data = res.data.data
    Object.keys(form).forEach((k) => { form[k] = data[k] || (k === 'currency' ? 'USD' : '') })
    // 非密钥字段回显; 密钥只标记已配置, 不回显
    ;['paypal_env', 'store_base_url', 'paypal_client_id', 'paypal_webhook_id'].forEach((k) => {
      if (data[k]) pay[k] = data[k]
    })
    pay.stripeConfigured = !!data.stripe_secret_key
    pay.stripeWebhookConfigured = !!data.stripe_webhook_secret
    pay.paypalSecretConfigured = !!data.paypal_client_secret
  } finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    await request.put('/api/v1/store-settings', { ...form })
    ElMessage.success('已保存')
  } finally { saving.value = false }
}

async function savePayment() {
  saving.value = true
  try {
    // 留空的密钥不覆盖已存值
    const body: Record<string, string> = { paypal_env: pay.paypal_env, store_base_url: pay.store_base_url }
    if (pay.paypal_client_id) body.paypal_client_id = pay.paypal_client_id
    if (pay.paypal_webhook_id) body.paypal_webhook_id = pay.paypal_webhook_id
    if (pay.stripe_secret_key) body.stripe_secret_key = pay.stripe_secret_key
    if (pay.stripe_webhook_secret) body.stripe_webhook_secret = pay.stripe_webhook_secret
    if (pay.paypal_client_secret) body.paypal_client_secret = pay.paypal_client_secret
    await request.put('/api/v1/store-settings', body)
    ElMessage.success('支付设置已保存 (即时生效)')
    pay.stripe_secret_key = ''; pay.stripe_webhook_secret = ''; pay.paypal_client_secret = ''
    load()
  } finally { saving.value = false }
}

// ---------- 店面主题 ----------
const themeInfo = ref<any>({ presets: [] })
const themeLoading = ref(false)
const themeSaving = ref(false)
const logoUploading = ref(false)
const previewKey = ref(0)
// 预览地址: 优先店面地址设置, 其次结算回跳地址, 兜底本地 8090 (开发环境)
const storefrontUrl = computed(
  () => form.storefront_url || pay.store_base_url || 'http://localhost:8090/',
)
const themeForm = reactive({
  preset: 'minimal', primary: '', bg: '', radiusNum: 8, font: 'sans', logoPath: '',
})

async function loadTheme() {
  themeLoading.value = true
  try {
    const res: any = await request.get('/api/v1/theme')
    themeInfo.value = res.data.data
    const o = res.data.data.overrides || {}
    themeForm.preset = res.data.data.preset || 'minimal'
    themeForm.primary = o.primary || ''
    themeForm.bg = o.bg || ''
    themeForm.radiusNum = o.radius ? parseInt(o.radius) : 8
    themeForm.font = o.font || 'sans'
    themeForm.logoPath = (res.data.data.tokens || {}).logoPath || ''
  } finally { themeLoading.value = false }
}

async function uploadLogo(file: any) {
  logoUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await request.post('/api/v1/media/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    themeForm.logoPath = res.data.data.path
  } finally { logoUploading.value = false }
}

/** 选择预设: 控件同步为该预设默认值 (清空颜色覆盖), 商家可再显式微调 */
function selectPreset(p: any) {
  themeForm.preset = p.name
  themeForm.font = p.font || 'sans'
  themeForm.radiusNum = parseInt(p.radius) || 8
  themeForm.primary = ''
  themeForm.bg = ''
}

async function saveTheme() {  themeSaving.value = true
  try {
    const overrides: Record<string, string> = {}
    if (themeForm.primary) overrides.primary = themeForm.primary
    if (themeForm.bg) overrides.bg = themeForm.bg
    overrides.radius = String(themeForm.radiusNum)
    overrides.font = themeForm.font
    await request.put('/api/v1/theme', { preset: themeForm.preset, overrides })
    ElMessage.success('主题已保存, 店面即时生效')
    previewKey.value++
    loadTheme()
  } finally { themeSaving.value = false }
}

onMounted(() => { load(); loadTheme() })
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
