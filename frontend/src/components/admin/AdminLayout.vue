<template>
  <div class="admin-container" v-if="!isLoginPage">
    <el-container>
      <el-aside class="admin-aside" width="220px">
        <div class="logo">
          <div class="logo-mark">
            <el-icon><Food /></el-icon>
          </div>
          <h2 class="logo-title">美食推荐系统</h2>
          <p>管理后台</p>
        </div>
        <el-menu
          class="admin-menu"
          :default-active="activeMenu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/recipes">
            <el-icon><Food /></el-icon>
            <span>食谱管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/categories">
            <el-icon><List /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/attributes">
            <el-icon><Setting /></el-icon>
            <span>属性管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><Document /></el-icon>
            <span>系统日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="admin-header">
          <div class="header-content">
            <div class="header-left">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
                <el-breadcrumb-item v-if="currentRouteName">{{ currentRouteName }}</el-breadcrumb-item>
              </el-breadcrumb>
              <div class="page-title">{{ currentRouteName || '管理后台' }}</div>
            </div>
            <div class="user-info">
              <el-dropdown>
                <div class="user-dropdown">
                  <el-avatar :size="32" icon="User" />
                  <span class="username">{{ adminInfo.username || '管理员' }}</span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>个人信息</el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>

        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
  <div v-else>
    <router-view />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Food, DataAnalysis, List, Document, Setting } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const adminInfo = ref({
  username: 'admin',
  email: 'admin@example.com'
})

const isLoginPage = computed(() => route.path === '/admin/login')

const activeMenu = computed(() => {
  if (route.path.includes('/admin/dashboard')) return '/admin/dashboard'
  if (route.path.includes('/admin/users')) return '/admin/users'
  if (route.path.includes('/admin/recipes')) return '/admin/recipes'
  if (route.path.includes('/admin/categories')) return '/admin/categories'
  if (route.path.includes('/admin/attributes')) return '/admin/attributes'
  if (route.path.includes('/admin/logs')) return '/admin/logs'
  return route.path
})

const currentRouteName = computed(() => {
  const routeMap = {
    '/admin/dashboard': '数据统计',
    '/admin/users': '用户管理',
    '/admin/recipes': '食谱管理',
    '/admin/categories': '分类管理',
    '/admin/attributes': '属性管理',
    '/admin/logs': '系统日志'
  }
  return routeMap[route.path] || ''
})

const loadAdminInfo = async () => {
  try {
    const storedInfo = localStorage.getItem('admin_info')
    if (storedInfo) {
      adminInfo.value = JSON.parse(storedInfo)
    }
    
    const res = await adminApi.getProfile()
    if (res.code === 200 && res.data) {
      adminInfo.value = res.data
      localStorage.setItem('admin_info', JSON.stringify(res.data))
    }
  } catch (error) {
    console.error('加载管理员信息失败:', error)
  }
}

const handleLogout = async () => {
  try {
    await adminApi.logout()
  } catch (error) {
    console.error('退出登录失败:', error)
  } finally {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    router.push('/admin/login')
  }
}

onMounted(() => {
  if (!isLoginPage.value) {
    loadAdminInfo()
  }
})
</script>

<style scoped>
.admin-container {
  height: 100vh;
  overflow: hidden;
  background: radial-gradient(circle at 5% 5%, #fff1f1 0%, #f5f7fa 30%);
}

.el-container {
  height: 100%;
}

.admin-aside {
  background: linear-gradient(180deg, #2d3d4f 0%, #223140 100%);
  color: #fff;
  overflow-x: hidden;
  box-shadow: 6px 0 18px rgba(8, 15, 25, 0.16);
}

.logo {
  min-height: 88px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 4px;
  background: linear-gradient(180deg, #253647 0%, #1f2d3c 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}

.logo h2 {
  margin: 0;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #ced7e1;
}

.el-menu {
  border-right: none;
  padding: 8px;
}

.admin-menu .el-menu-item {
  border-radius: 10px;
  margin-bottom: 6px;
}

.admin-menu .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.15) !important;
  color: #ffffff !important;
}

.admin-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

.admin-header {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 6px 18px rgba(0, 21, 41, 0.06);
  padding: 0 24px;
  display: flex;
  align-items: center;
  z-index: 10;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.header-left {
  flex: 1;
  min-width: 0;
}

.page-title {
  margin-top: 6px;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-dropdown:hover {
  border-color: rgba(64, 158, 255, 0.35);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.12);
}

.username {
  margin-left: 8px;
  color: #606266;
  font-size: 14px;
}

.admin-main {
  background: linear-gradient(180deg, #f8fafc 0%, #f5f7fa 100%);
  padding: 24px;
}

@media (max-width: 1100px) {
  .admin-aside {
    width: 76px !important;
  }

  .logo-title,
  .logo p,
  .admin-menu .el-menu-item span {
    display: none;
  }

  .logo {
    min-height: 64px;
  }
}

@media (max-width: 768px) {
  .admin-main {
    padding: 14px;
  }

  .admin-header {
    padding: 0 12px;
  }

  .page-title {
    font-size: 15px;
  }

  .username {
    display: none;
  }
}
</style>
