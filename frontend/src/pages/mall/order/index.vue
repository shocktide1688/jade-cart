<template>
  <div>
    <el-card>
      <template #header><span>订单管理</span></template>
      <div style="display:flex;gap:8px;margin-bottom:12px">
        <el-input v-model="query.keyword" placeholder="订单号/邮箱" style="width:220px" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="状态" style="width:150px" clearable>
          <el-option v-for="(label, key) in statusText" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="load">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="orderNumber" label="订单号" width="180" />
        <el-table-column prop="email" label="邮箱" min-width="170" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusText[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">{{ money(row.totalCents) }} {{ row.currency }}</template>
        </el-table-column>
        <el-table-column prop="paymentProvider" label="支付渠道" width="90" />
        <el-table-column prop="createdAt" label="下单时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.publicId)">详情</el-button>
            <el-button v-if="row.status === 'PAID'" link type="success" @click="openFulfill(row)">履约</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="warning" @click="act(row, 'cancel', '取消')">取消</el-button>
            <el-button v-if="['PAID', 'PAYMENT_REVIEW'].includes(row.status)" link type="danger" @click="act(row, 'refund', '退款')">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" v-model:current-page="query.page" @current-change="load" />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.orderNumber }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType[detail.status]">{{ statusText[detail.status] || detail.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ detail.shipName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付渠道">{{ detail.paymentProvider || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付流水号">{{ detail.paymentRef || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预留截止">{{ detail.reservationExpiresAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detail.paidAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货国家">{{ detail.shippingCountry || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-if="detail.shipment" :column="2" border size="small" style="margin-top:12px" title="物流">
          <el-descriptions-item label="承运商">{{ detail.shipment.carrier || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运单号">{{ detail.shipment.trackingNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="追踪链接" :span="2">
            <a v-if="detail.shipment.trackingUrl" :href="detail.shipment.trackingUrl" target="_blank">{{ detail.shipment.trackingUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" size="small" border style="margin-top:12px">
          <el-table-column prop="productTitle" label="商品" min-width="150" />
          <el-table-column prop="variantSpec" label="规格" min-width="110" />
          <el-table-column prop="sku" label="SKU" min-width="120" />
          <el-table-column label="单价" width="90">
            <template #default="{ row }">{{ money(row.unitPriceCents) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="60" />
          <el-table-column label="小计" width="90">
            <template #default="{ row }">{{ money(row.lineTotalCents) }}</template>
          </el-table-column>
        </el-table>
        <p style="text-align:right;font-weight:600">
          合计: {{ money(detail.totalCents) }} {{ detail.currency }}
          (含运费 {{ money(detail.shippingCents) }})
        </p>
      </template>
    </el-dialog>

    <!-- 履约: 物流录入 -->
    <el-dialog v-model="fulfillVisible" :title="'履约 - ' + (fulfillOrder?.orderNumber || '')" width="460px">
      <el-form label-width="100">
        <el-form-item label="承运商">
          <el-input v-model="fulfillForm.carrier" placeholder="USPS / UPS / DHL..." />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="fulfillForm.trackingNumber" />
        </el-form-item>
        <el-form-item label="追踪链接">
          <el-input v-model="fulfillForm.trackingUrl" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fulfillVisible = false">取消</el-button>
        <el-button type="primary" :loading="fulfilling" @click="doFulfill">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const detailVisible = ref(false)
const detail = ref<any>(null)
const fulfillVisible = ref(false)
const fulfilling = ref(false)
const fulfillOrder = ref<any>(null)
const fulfillForm = reactive({ carrier: '', trackingNumber: '', trackingUrl: '' })

const statusText: Record<string, string> = {
  PENDING: '待支付', PAID: '已支付', FULFILLED: '已履约',
  CANCELLED: '已取消', REFUNDED: '已退款', PAYMENT_REVIEW: '支付待审核',
}
const statusType: Record<string, string> = {
  PENDING: 'warning', PAID: 'success', FULFILLED: 'primary',
  CANCELLED: 'info', REFUNDED: 'info', PAYMENT_REVIEW: 'danger',
}

const money = (c: number) => '$' + (c / 100).toFixed(2)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/api/v1/orders', { params: query })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

async function openDetail(publicId: string) {
  detail.value = await request.get(`/api/v1/orders/${publicId}`)
  detailVisible.value = true
}

function openFulfill(row: any) {
  fulfillOrder.value = row
  Object.assign(fulfillForm, { carrier: '', trackingNumber: '', trackingUrl: '' })
  fulfillVisible.value = true
}

async function doFulfill() {
  fulfilling.value = true
  try {
    await request.post(`/api/v1/orders/${fulfillOrder.value.publicId}/fulfill`, fulfillForm)
    ElMessage.success('已履约并发货通知')
    fulfillVisible.value = false
    load()
  } finally { fulfilling.value = false }
}

async function act(row: any, action: string, label: string) {
  await ElMessageBox.confirm(`确认对订单 ${row.orderNumber} 执行「${label}」?`)
  await request.post(`/api/v1/orders/${row.publicId}/${action}`)
  ElMessage.success(`已${label}`)
  load()
}

onMounted(load)
</script>
