<template>
  <template v-if="isAdminPage">
    <router-view />
  </template>
  <template v-else>
    <el-container class="app-container">
      <el-header class="app-header">
        <div class="header-content">
          <el-button class="mobile-menu-btn" text circle @click="mobileMenuVisible = true">
            <el-icon :size="20">
              <Menu />
            </el-icon>
          </el-button>
          <div class="logo" @click="$router.push('/')">
            <div class="logo-icon">
              <el-icon :size="22">
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
            <el-menu-item index="/create-recipe">
              <el-icon>
                <EditPen />
              </el-icon>
              发布
            </el-menu-item>
            <el-menu-item index="/admin/login">
              <el-icon>
                <Setting />
              </el-icon>
              管理后台
            </el-menu-item>
          </el-menu>
          <div class="header-scene-mode">
            <SceneModeTopNav />
          </div>
          <div class="header-right">
            <div class="search-wrapper">
              <SearchEntry
                v-model="searchKeyword"
                source-page="header"
                placeholder="搜索菜名、食材、作者..."
                @submit="handleSearchSubmit"
              />
            </div>
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="click">
                <div class="user-info">
                  <el-avatar :size="34" class="user-avatar">{{ userStore.user?.username?.charAt(0) || 'U' }}</el-avatar>
                  <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                    <el-dropdown-item @click="openOnboarding">
                      <el-icon>
                        <EditPen />
                      </el-icon>兴趣标签
                    </el-dropdown-item>
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
                <span class="login-btn-text">登录</span>
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

    <el-drawer v-model="mobileMenuVisible" direction="ltr" size="86%" class="mobile-nav-drawer" :with-header="false">
      <div class="mobile-drawer-content">
        <div class="mobile-brand" @click="goTo('/')">
          <div class="logo-icon">
            <el-icon :size="22">
              <Food />
            </el-icon>
          </div>
          <span>美食推荐</span>
        </div>

        <div class="mobile-search">
          <SearchEntry
            v-model="searchKeyword"
            source-page="header"
            placeholder="搜索菜名、食材、作者..."
            @submit="handleSearchSubmit"
          />
        </div>

        <el-menu class="mobile-nav-menu" :default-active="activeMobileNav" @select="handleMobileMenuSelect">
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
          <el-menu-item index="/create-recipe">
            <el-icon>
              <EditPen />
            </el-icon>
            发布菜谱
          </el-menu-item>
          <el-menu-item index="/admin/login">
            <el-icon>
              <Setting />
            </el-icon>
            管理后台
          </el-menu-item>
        </el-menu>
        <div class="mobile-scene-mode">
          <SceneModeTopNav />
        </div>

        <div class="mobile-user-panel">
          <template v-if="userStore.isLoggedIn">
            <div class="mobile-user-info">
              <el-avatar :size="34" class="user-avatar">{{ userStore.user?.username?.charAt(0) || 'U' }}</el-avatar>
              <div class="mobile-user-text">
                <strong>{{ userStore.user?.username || '用户' }}</strong>
                <span>欢迎回来</span>
              </div>
            </div>
            <div class="mobile-user-actions">
              <el-button @click="goTo('/user')">个人中心</el-button>
              <el-button @click="goTo('/user/favorites')">我的收藏</el-button>
              <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
            </div>
          </template>
          <template v-else>
            <div class="mobile-user-actions">
              <el-button type="primary" @click="goTo('/login')">登录</el-button>
              <el-button @click="goTo('/register')">注册</el-button>
            </div>
          </template>
        </div>
      </div>
    </el-drawer>

    <OnboardingSurveyDialog v-model="showOnboardingDialog" @completed="handleOnboardingCompleted" />
  </template>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Food, HomeFilled, Dish, Star, User, CollectionTag, SwitchButton, Setting, Menu, EditPen } from '@element-plus/icons-vue'
import { useSceneModeStore } from '@/stores/sceneMode'
import { useUserStore } from '@/stores/user'
import OnboardingSurveyDialog from '@/components/OnboardingSurveyDialog.vue'
import SearchEntry from '@/components/SearchEntry.vue'
import SceneModeTopNav from '@/components/SceneModeTopNav.vue'

const route = useRoute()
const router = useRouter()
const sceneModeStore = useSceneModeStore()
const userStore = useUserStore()
const searchKeyword = ref('')
const mobileMenuVisible = ref(false)
const showOnboardingDialog = ref(false)

let hasAppliedSceneTheme = false
const ensureSceneTheme = () => {
  if (hasAppliedSceneTheme) return
  sceneModeStore.initializeTheme()
  hasAppliedSceneTheme = true
}

if (typeof window !== 'undefined') {
  ensureSceneTheme()
}

const isAdminPage = computed(() => {
  return route.path.startsWith('/admin')
})

const activeMobileNav = computed(() => {
  if (route.path.startsWith('/recipes')) return '/recipes'
  if (route.path.startsWith('/recommend')) return '/recommend'
  if (route.path.startsWith('/create-recipe')) return '/create-recipe'
  if (route.path.startsWith('/admin')) return '/admin/login'
  return '/'
})

watch(() => route.fullPath, () => {
  mobileMenuVisible.value = false
})

watch(() => [route.path, route.query.keyword], ([path, value]) => {
  if (path === '/search') {
    searchKeyword.value = typeof value === 'string' ? value.trim() : ''
  }
}, { immediate: true })

const handleSearchSubmit = ({ keyword }) => {
  mobileMenuVisible.value = false
  router.push({ path: '/search', query: { keyword } })
}

const goTo = (path) => {
  mobileMenuVisible.value = false
  router.push(path)
}

const handleMobileMenuSelect = (index) => {
  goTo(index)
}

const openOnboarding = () => {
  showOnboardingDialog.value = true
}

const handleOnboardingCompleted = async () => {
  await userStore.fetchProfile()
}

const handleLogout = () => {
  userStore.logout()
  showOnboardingDialog.value = false
  mobileMenuVisible.value = false
  router.push('/')
}

onMounted(async () => {
  ensureSceneTheme()
  if (!userStore.token) return
  await userStore.fetchProfile()
  if (userStore.user && userStore.user.onboardingCompleted === false) {
    showOnboardingDialog.value = true
  }
})

watch(() => userStore.user, (user) => {
  if (!userStore.isLoggedIn) {
    showOnboardingDialog.value = false
    return
  }
  if (user && user.onboardingCompleted === false) {
    showOnboardingDialog.value = true
  }
})
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
  height: 66px;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1380px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 18px;
  gap: 8px;
  min-width: 0;
}

.mobile-menu-btn {
  display: none;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: var(--transition);
  flex-shrink: 0;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  line-height: 1;
  display: inline-block;
}

.nav-menu {
  flex: 1 1 320px;
  border-bottom: none;
  margin-left: 10px;
  background: transparent;
  min-width: 0;
  overflow: hidden;
}

.nav-menu .el-menu-item {
  height: 66px;
  line-height: 66px;
  font-size: 12px;
  font-weight: 500;
  padding: 0 8px;
  border-bottom: 3px solid transparent;
  transition: var(--transition);
}

.nav-menu :deep(.el-menu-item .el-icon) {
  margin-right: 3px;
  font-size: 14px;
}

.nav-menu .el-menu-item:hover {
  background: rgba(255, 107, 107, 0.05);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  min-width: 0;
  flex: 0 1 auto;
}

.header-scene-mode {
  flex: 0 1 390px;
  min-width: 0;
  max-width: min(390px, 30vw);
  overflow: hidden;
}

.header-scene-mode :deep(.scene-top-nav) {
  width: 100%;
}

.search-wrapper {
  width: clamp(130px, 15vw, 220px);
  min-width: 0;
  flex: 0 1 220px;
}

.search-wrapper :deep(.el-input__wrapper) {
  border-radius: 20px;
  padding: 0 12px;
}

.search-wrapper :deep(.search-entry__button) {
  border-radius: 20px;
  padding: 8px 16px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 20px;
  transition: var(--transition);
  min-width: 0;
  max-width: 120px;
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
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  max-width: 64px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
}

.login-btn {
  border-radius: 20px;
  padding: 8px 16px;
}

.app-main {
  background: var(--bg-color);
  padding: 20px 0;
  min-height: calc(100vh - 66px - 168px);
}

.app-footer {
  background: #17202a;
  height: auto;
  padding: 32px 0 20px;
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

.mobile-drawer-content {
  height: 100%;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.mobile-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  cursor: pointer;
}

.mobile-search {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.mobile-nav-menu {
  border-right: none;
  margin: 6px 0 12px;
}

.mobile-nav-menu .el-menu-item {
  border-radius: 10px;
  margin-bottom: 4px;
}

.mobile-scene-mode {
  margin: 4px 0 12px;
}

.mobile-scene-mode :deep(.scene-top-nav) {
  width: 100%;
  flex-wrap: wrap;
  border-radius: var(--radius-md);
}

.mobile-user-panel {
  margin-top: auto;
  padding-top: 14px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--border-color);
}

.mobile-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.mobile-user-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.mobile-user-text strong {
  color: var(--text-primary);
  font-size: 14px;
}

.mobile-user-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.mobile-user-actions .el-button {
  width: 100%;
}

:deep(.mobile-nav-drawer .el-drawer__body) {
  padding: 0;
}

@media (max-width: 1440px) {
  .header-content {
    padding: 0 14px;
    gap: 6px;
  }

  .logo-text {
    font-size: 17px;
  }

  .nav-menu {
    margin-left: 8px;
  }

  .nav-menu .el-menu-item {
    padding: 0 7px;
    font-size: 11.5px;
  }

  .header-scene-mode {
    flex-basis: 350px;
    max-width: min(350px, 29vw);
  }

  .search-wrapper {
    width: clamp(120px, 13vw, 190px);
    flex-basis: 190px;
  }

  .user-info {
    max-width: 42px;
    padding: 4px;
    justify-content: center;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 1180px) {
  .nav-menu {
    margin-left: 6px;
  }

  .search-wrapper {
    width: clamp(112px, 12vw, 160px);
    flex-basis: 160px;
  }

  .logo-text {
    font-size: 16px;
  }

  .header-scene-mode {
    flex-basis: 300px;
    max-width: min(300px, 26vw);
  }

  .header-right {
    gap: 6px;
  }

  .nav-menu .el-menu-item {
    padding: 0 6px;
  }
}

@media (max-width: 900px) {
  .mobile-menu-btn {
    display: inline-flex;
  }

  .header-content {
    gap: 8px;
    padding: 0 14px;
  }

  .header-right {
    margin-left: auto;
    gap: 8px;
  }

  .nav-menu {
    display: none;
  }

  .header-scene-mode {
    display: none;
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

  .login-btn {
    padding: 8px 12px;
    min-height: 38px;
  }

  .user-info {
    padding: 4px 8px;
  }
}

@media (max-width: 640px) {
  .nav-menu {
    margin-left: 0;
  }

  .logo-icon {
    width: 34px;
    height: 34px;
  }

  .login-btn-text {
    display: none;
  }

  .login-btn {
    min-width: 36px;
    padding: 8px;
  }

  .mobile-search {
    gap: 10px;
  }

  .mobile-search :deep(.search-entry) {
    flex-direction: column;
  }

  .mobile-search :deep(.search-entry__button) {
    width: 100%;
  }

  .user-name {
    display: none;
  }

  .app-footer {
    padding: 22px 0 14px;
  }

  .footer-links {
    gap: 14px;
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 430px) {
  .mobile-drawer-content {
    padding: 16px 12px;
  }

  .mobile-nav-menu .el-menu-item {
    height: 44px;
    line-height: 44px;
  }
}
</style>

