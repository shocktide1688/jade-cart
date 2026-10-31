<template>
  <div>
    <el-card>
      <template #header><span>商城设置</span></template>
      <el-tabs v-model="tab">
        <!-- 店铺信息 -->
        <el-tab-pane label="店铺信息" name="shop">
          <el-form :model="form" label-width="120" style="max-width: 560px" v-loading="loading">
            <el-form-item label="店铺名称">
              <el-input v-model="form.store_name" />
            </el-form-item>
            <el-form-item label="货币">
              <el-select v-model="form.currency" style="width:160px">
                <el-option label="USD 美元" value="USD" />
                <el-option label="EUR 欧元" value="EUR" />
                <el-option label="CNY 人民币" value="CNY" />
              </el-select>
            </el-form-item>
            <el-form-item label="客服邮箱">
              <el-input v-model="form.support_email" placeholder="support@example.com" />
            </el-form-item>
            <el-form-item label="店面公告">
              <el-input v-model="form.announcement" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 支付设置 -->
        <el-tab-pane label="支付设置" name="payment">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px"
                    title="渠道选择逻辑" description="结账按 Stripe → PayPal → Mock 顺序选首个已配置密钥的渠道；全部未配置时走内置 Mock 收银台。" />
          <el-form label-width="160" style="max-width: 640px" v-loading="loading">
            <h4>Stripe</h4>
            <el-form-item label="Secret Key">
              <el-input v-model="pay.stripe_secret_key" type="password" show-password
                        :placeholder="pay.stripeConfigured ? '已配置 (输入新值覆盖)' : 'sk_test_... / sk_live_...'" />
            </el-form-item>
            <el-form-item label="Webhook Secret">
              <el-input v-model="pay.stripe_webhook_secret" type="password" show-password
                        :placeholder="pay.stripeWebhookConfigured ? '已配置 (输入新值覆盖)' : 'whsec_... (验签用)'" />
            </el-form-item>
            <el-form-item label="Webhook URL">
              <el-input :model-value="(pay.store_base_url || 'http://localhost:8090') + '/api/store/webhooks/stripe'" disabled />
            </el-form-item>

            <h4 style="margin-top:20px">PayPal</h4>
            <el-form-item label="Client ID">
              <el-input v-model="pay.paypal_client_id" />
            </el-form-item>
            <el-form-item label="Client Secret">
              <el-input v-model="pay.paypal_client_secret" type="password" show-password
                        :placeholder="pay.paypalSecretConfigured ? '已配置 (输入新值覆盖)' : ''" />
            </el-form-item>
            <el-form-item label="环境">
              <el-radio-group v-model="pay.paypal_env">
                <el-radio value="sandbox">Sandbox</el-radio>
                <el-radio value="live">Live</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="Webhook ID">
              <el-input v-model="pay.paypal_webhook_id" placeholder="验签用" />
            </el-form-item>

            <h4 style="margin-top:20px">结算回跳</h4>
            <el-form-item label="店面地址">
              <el-input v-model="pay.store_base_url" placeholder="http://localhost:8090" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="savePayment">保存支付设置</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="warning" :closable="false" show-icon
                    title="安全" description="所有密钥 AES-256-GCM 加密入库, 界面不回显明文。" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const tab = ref('shop')
const loading = ref(false)
const saving = ref(false)

const form = reactive<Record<string, string>>({
  store_name: '', currency: 'USD', support_email: '', announcement: '',
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

onMounted(load)
</script>
