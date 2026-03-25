<template>
  <div class="user-report container page-shell">
    <div class="page-heading">
      <h2 class="title">个人 7 日饮食报告</h2>
      <p class="subtitle">了解你的烹饪节奏与偏好变化</p>
    </div>
    <el-row :gutter="24">
      <el-col :xs="24" :md="7" :lg="6">
        <el-card class="menu-card">
          <div class="user-info">
            <el-avatar :size="64" :src="userStore.user?.avatar">{{ userStore.user?.nickname?.charAt(0) || 'U' }}</el-avatar>
            <h3>{{ userStore.user?.nickname || '用户' }}</h3>
          </div>
          <el-menu :default-active="activeMenu" router>
            <el-menu-item index="/user">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="/user/favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="/user/report">
              <el-icon><DataAnalysis /></el-icon>
              <span>7日报告</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="17" :lg="18">
        <el-card class="report-card">
          <template #header>
            <div class="card-header">
              <span>周期：{{ report?.periodStart }} ~ {{ report?.periodEnd }}</span>
              <el-tag type="success" effect="light">自动生成</el-tag>
            </div>
          </template>
          <el-skeleton v-if="loading" :rows="8" animated />
          <template v-else-if="report">
            <p class="summary">{{ report.summary }}</p>

            <el-row :gutter="12" class="stats-row">
              <el-col :xs="24" :sm="8">
                <div class="stat-box">
                  <div class="stat-label">开启烹饪</div>
                  <div class="stat-value">{{ report.startedCookingCount }}</div>
                </div>
              </el-col>
              <el-col :xs="24" :sm="8">
                <div class="stat-box">
                  <div class="stat-label">完成次数</div>
                  <div class="stat-value">{{ report.completedCookingCount }}</div>
                </div>
              </el-col>
              <el-col :xs="24" :sm="8">
                <div class="stat-box">
                  <div class="stat-label">完成率</div>
                  <div class="stat-value">{{ report.completionRate }}%</div>
                </div>
              </el-col>
            </el-row>

            <div class="section-block">
              <h4>场景偏好</h4>
              <el-tag
                v-for="item in report.scenePreferences || []"
                :key="item.code"
                class="tag-item"
                type="info"
              >
                {{ item.name }} · {{ item.count }}
              </el-tag>
              <el-empty v-if="!(report.scenePreferences || []).length" :image-size="70" description="暂无场景数据" />
            </div>

            <div class="section-block">
              <h4>口味偏好</h4>
              <el-tag
                v-for="item in report.tastePreferences || []"
                :key="item.code"
                class="tag-item"
                type="warning"
              >
                {{ item.name }} · {{ item.count }}
              </el-tag>
              <el-empty v-if="!(report.tastePreferences || []).length" :image-size="70" description="暂无口味数据" />
            </div>

            <div class="section-block">
              <h4>活跃时段</h4>
              <el-tag
                v-for="item in report.activeHours || []"
                :key="item.label"
                class="tag-item"
                type="success"
              >
                {{ item.label }} · {{ item.count }}
              </el-tag>
              <el-empty v-if="!(report.activeHours || []).length" :image-size="70" description="暂无活跃时段数据" />
            </div>

            <div class="section-block">
              <h4>建议</h4>
              <el-alert
                v-for="(tip, idx) in report.suggestions || []"
                :key="idx"
                :title="tip"
                type="success"
                :closable="false"
                show-icon
                class="tip-item"
              />
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, Star, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = ref('/user/report')
const loading = ref(false)
const report = ref(null)

const fetchReport = async () => {
  loading.value = true
  try {
    const res = await userApi.get7dReport()
    report.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('报告加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  activeMenu.value = route.path
  await userStore.fetchProfile()
  await fetchReport()
})
</script>

<style scoped>
.user-report {
  padding-top: 0;
}

.page-heading {
  margin-bottom: 14px;
}

.title {
  font-size: 26px;
  line-height: 1.2;
  color: var(--text-primary);
}

.subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.menu-card {
  min-height: 400px;
}

.user-info {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
}

.user-info h3 {
  margin-top: 12px;
  font-size: 16px;
}

.el-menu {
  border-right: none;
}

.report-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.summary {
  color: var(--text-primary);
  line-height: 1.8;
  margin-bottom: 14px;
}

.stats-row {
  margin-bottom: 12px;
}

.stat-box {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 14px;
  background: #fff;
}

.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
}

.section-block {
  margin-top: 16px;
}

.section-block h4 {
  margin-bottom: 10px;
  color: var(--text-primary);
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}

.tip-item {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .title {
    font-size: 22px;
  }

  .menu-card {
    min-height: auto;
    margin-bottom: 12px;
  }

  .report-card {
    min-height: auto;
  }
}
</style>
