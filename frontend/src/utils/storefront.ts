/**
 * 店面地址 (跨页面共用): 博客/内容页"查看"链接、店面装修与商城设置的实时预览都用它。
 * 取值: storefront_url → store_base_url → 本地 8090 兜底。
 * 生产环境必须在 商城设置-店铺信息 配置店面地址, 否则所有店面链接指向管理员本机。
 */
import { computed, ref } from 'vue'
import request from './request'

const storeBaseUrl = ref('')
let loaded = false

/** 拉取店面地址设置 (幂等: 多页面调用只请求一次) */
export function initStorefrontUrl() {
  if (loaded) return
  loaded = true
  request
    .get('/api/v1/store-settings')
    .then((r: any) => {
      const d = r.data?.data || {}
      storeBaseUrl.value = d.storefront_url || d.store_base_url || ''
    })
    .catch(() => {})
}

/** 店面基址 (模板里直接绑定, 拼 /blog/{slug} /pages/{slug} 等路径) */
export const storefrontBase = computed(() => storeBaseUrl.value || 'http://localhost:8090/')
