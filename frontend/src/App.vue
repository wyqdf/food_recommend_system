<template>
  <template v-if="isAdminPage">
    <router-view />
  </template>
  <template v-else>
    <el-container class="app-container">
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <el-icon :size="24">
                <Food />
              </el-icon>
            </div>
            <span class="logo-text">美食推荐</span>
          </div>
          <el-menu mode="horizontal" :ellipsis="false" class="nav-menu" router>
            <el-menu-item index="/">
              <el-icon>
                <HomeFilled />
              </el-icon>
              首页
            </el-menu-item>
            <el-menu-item index="/recipes">
              <el-icon>
                <Dish />
              </el-icon>
              菜谱
            </el-menu-item>
            <el-menu-item index="/recommend">
              <el-icon>
                <Star />
              </el-icon>
              推荐
            </el-menu-item>
            <el-menu-item index="/admin/login">
              <el-icon>
                <Setting />
              </el-icon>
              管理后台
            </el-menu-item>
          </el-menu>
          <div class="header-right">
            <div class="search-wrapper">
              <el-input v-model="searchKeyword" placeholder="搜索菜谱..." class="search-input" @keyup.enter="handleSearch"
                clearable>
                <template #prefix>
                  <el-icon>
                    <Search />
                  </el-icon>
                </template>
              </el-input>
              <el-button type="primary" class="search-btn" @click="handleSearch">搜索</el-button>
            </div>
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="click">
                <div class="user-info">
                  <el-avatar :size="36" class="user-avatar">{{ userStore.user?.username?.charAt(0) || 'U' }}</el-avatar>
                  <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="$router.push('/user')">
                      <el-icon>
                        <User />
                      </el-icon>个人中心
                    </el-dropdown-item>
                    <el-dropdown-item @click="$router.push('/user/favorites')">
                      <el-icon>
                        <CollectionTag />
                      </el-icon>我的收藏
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">
                      <el-icon>
                        <SwitchButton />
                      </el-icon>退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" class="login-btn" @click="$router.push('/login')">
                <el-icon>
                  <User />
                </el-icon>
                登录
              </el-button>
            </template>
          </div>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
      <el-footer class="app-footer">
        <div class="footer-content">
          <div class="footer-info">
            <div class="footer-logo">
              <el-icon :size="20">
                <Food />
              </el-icon>
              <span>美食推荐</span>
            </div>
            <p class="footer-desc">发现美食，享受烹饪，让每一餐都精彩</p>
          </div>
          <div class="footer-links">
            <a href="#">关于我们</a>
            <a href="#">联系方式</a>
            <a href="#">帮助中心</a>
          </div>
          <div class="footer-copyright">
            <p>© 2026 美食推荐系统 All Rights Reserved</p>
          </div>
        </div>
      </el-footer>
    </el-container>
  </template>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Food, HomeFilled, Dish, Star, User, CollectionTag, SwitchButton, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')

const isAdminPage = computed(() => {
  return route.path.startsWith('/admin')
})

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}

.app-header {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
  height: 70px;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: var(--page-width);
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 14px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: var(--transition);
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
  margin-left: 36px;
  background: transparent;
}

.nav-menu .el-menu-item {
  height: 70px;
  line-height: 70px;
  font-size: 15px;
  font-weight: 500;
  border-bottom: 3px solid transparent;
  transition: var(--transition);
}

.nav-menu .el-menu-item:hover {
  background: rgba(255, 107, 107, 0.05);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 230px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  padding: 0 15px;
}

.search-btn {
  border-radius: 20px;
  padding: 8px 20px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: var(--transition);
}

.user-info:hover {
  background: rgba(255, 107, 107, 0.1);
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  color: #fff;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.login-btn {
  border-radius: 20px;
  padding: 10px 24px;
}

.app-main {
  background: var(--bg-color);
  padding: 24px 0;
  min-height: calc(100vh - 70px - 180px);
}

.app-footer {
  background: #17202a;
  height: auto;
  padding: 40px 0 24px;
  border-top: none;
}

.footer-content {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.footer-info {
  text-align: center;
}

.footer-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.footer-desc {
  color: #94a3b8;
  font-size: 14px;
}

.footer-links {
  display: flex;
  gap: 32px;
}

.footer-links a {
  color: #94a3b8;
  font-size: 14px;
  transition: var(--transition);
}

.footer-links a:hover {
  color: #fff;
}

.footer-copyright {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  width: 100%;
  text-align: center;
}

.footer-copyright p {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .nav-menu {
    margin-left: 10px;
  }

  .search-input {
    width: 180px;
  }

  .logo-text {
    font-size: 19px;
  }
}

@media (max-width: 900px) {
  .header-content {
    gap: 8px;
    padding: 0 12px;
  }

  .search-wrapper {
    display: none;
  }

  .nav-menu .el-menu-item {
    padding: 0 10px;
  }

  .logo-text {
    display: none;
  }
}

@media (max-width: 640px) {
  .nav-menu {
    margin-left: 0;
  }

  .user-name {
    display: none;
  }

  .app-footer {
    padding: 24px 0 16px;
  }

  .footer-links {
    gap: 14px;
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
