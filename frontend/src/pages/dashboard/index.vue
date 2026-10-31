<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { Document, Key, UserFilled, OfficeBuilding } from '@element-plus/icons-vue'

const userStore = useUserStore()
const stats = ref({
  users: 0, roles: 0, depts: 0, tenants: 0, patients: 0, projects: 0,
  operLogs: 0, loginLogs: 0, notices: 0,
})
const recentNotices = ref<any[]>([])
const recentOperLogs = ref<any[]>([])

async function load() {
  try {
    const [users, roles, depts, tenants, projects, operLogs, loginLogs, notices] = await Promise.all([
      request({ url: '/api/v1/users/page', method: 'GET', params: { page: 1, size: 1 } }),
      request({ url: '/api/v1/roles/all', method: 'GET' }),
      request({ url: '/api/v1/depts/all', method: 'GET' }),
      request({ url: '/api/v1/tenants', method: 'GET' }),
      request({ url: '/api/v1/projects', method: 'GET' }),
      request({ url: '/api/v1/log/oper/page', method: 'GET', params: { page: 1, size: 1 } }),
      request({ url: '/api/v1/log/login/page', method: 'GET', params: { page: 1, size: 1 } }),
      request({ url: '/api/v1/notices/latest', method: 'GET' }),
    ])
    stats.value = {
      users: users.data.data.total,
      roles: (roles.data.data || []).length,
      depts: (depts.data.data || []).length,
      tenants: (tenants.data.data || []).length,
      patients: 0, // 没 list 接口, 暂用 0
      projects: (projects.data.data || []).length,
      operLogs: operLogs.data.data.total,
      loginLogs: loginLogs.data.data.total,
      notices: (notices.data.data || []).length,
    }
    recentNotices.value = notices.data.data || []
    // 操作日志最新 5 条
    const oper = await request({ url: '/api/v1/log/oper/page', method: 'GET', params: { page: 1, size: 5 } })
    recentOperLogs.value = oper.data.data.records || []
  } catch (e) { console.error('Dashboard load failed', e) }
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <el-card class="welcome">
      <h2>欢迎回来，{{ userStore.userInfo?.nickname || userStore.username || '管理员' }} 👋</h2>
      <p>这是基于 Quarkus 3.33 LTS + Vue 3 的 Jade 管理后台，启动 0.9s，镜像 283MB</p>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#409eff"><UserFilled /></el-icon>
          <div class="stat-value">{{ stats.users }}</div>
          <div class="stat-label">用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#67c23a"><Key /></el-icon>
          <div class="stat-value">{{ stats.roles }}</div>
          <div class="stat-label">角色</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#e6a23c"><OfficeBuilding /></el-icon>
          <div class="stat-value">{{ stats.depts }}</div>
          <div class="stat-label">部门</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#f56c6c"><UserFilled /></el-icon>
          <div class="stat-value">{{ stats.tenants }}</div>
          <div class="stat-label">租户</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#909399"><UserFilled /></el-icon>
          <div class="stat-value">{{ stats.projects }}</div>
          <div class="stat-label">项目</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#9c27b0"><Document /></el-icon>
          <div class="stat-value">{{ stats.operLogs }}</div>
          <div class="stat-label">操作日志</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#ff5722"><Key /></el-icon>
          <div class="stat-value">{{ stats.loginLogs }}</div>
          <div class="stat-label">登录日志</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-icon class="stat-icon" :size="32" color="#00a86b"><Document /></el-icon>
          <div class="stat-value">{{ stats.notices }}</div>
          <div class="stat-label">最新公告</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight: 600;">最新公告</span></template>
          <el-table :data="recentNotices" size="small" empty-text="暂无公告">
            <el-table-column prop="noticeTitle" label="标题" show-overflow-tooltip />
            <el-table-column prop="noticeType" label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="row.noticeType === 1 ? 'primary' : 'warning'" size="small">
                  {{ row.noticeType === 1 ? '通知' : '公告' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="发布人" width="100" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span style="font-weight: 600;">最近操作</span></template>
          <el-table :data="recentOperLogs" size="small" empty-text="暂无操作">
            <el-table-column prop="title" label="模块" width="100" />
            <el-table-column prop="method" label="方法" show-overflow-tooltip />
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column prop="durationMs" label="ms" width="60" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }
.welcome h2 { margin: 0 0 8px; color: #303133; }
.welcome p { margin: 0; color: #909399; font-size: 14px; }
.stat-card { text-align: center; }
.stat-card :deep(.el-card__body) { padding: 18px 12px; }
.stat-icon { margin-bottom: 6px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; line-height: 1.2; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
</style>
