<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="login-brand">
          <div class="brand-icon">
            <el-icon :size="48">
              <Food />
            </el-icon>
          </div>
          <h1>美食推荐</h1>
          <p>发现美食，享受烹饪，让每一餐都精彩</p>
        </div>
        <div class="login-features">
          <div class="feature-item">
            <el-icon :size="24">
              <Search />
            </el-icon>
            <span>海量菜谱</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24">
              <Star />
            </el-icon>
            <span>智能推荐</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24">
              <CollectionTag />
            </el-icon>
            <span>收藏分享</span>
          </div>
        </div>
      </div>
      <div class="login-right">
        <el-card class="login-card">
          <div class="card-header">
            <h2>欢迎回来</h2>
            <p>登录您的账号</p>
          </div>
          <el-form ref="formRef" class="login-form" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" prefix-icon="User" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" prefix-icon="Lock" placeholder="请输入密码" size="large"
                show-password />
            </el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            </div>
            <el-form-item>
              <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading" size="large">
                登录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="demo-users">
            <div class="demo-users__header">
              <h3>展示用户</h3>
              <p>以下 5 个账号仅用于测试与演示，密码统一为 <strong>123456</strong></p>
            </div>
            <div class="demo-users__grid">
              <button
                v-for="user in demoUsers"
                :key="user.username"
                type="button"
                class="demo-user"
                @click="fillDemoUser(user.username)"
              >
                <span class="demo-user__name">{{ user.username }}</span>
              </button>
            </div>
          </div>
          <div class="register-link">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Food, Search, Star, CollectionTag } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)
const demoUsers = [
  { username: '好吃好吃难做' },
  { username: '梅依旧' },
  { username: '千惠烘焙' },
  { username: '小红马' },
  { username: 'littlelittle' }
]

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const fillDemoUser = (username) => {
  form.username = username
  form.password = '123456'
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    
    if (rememberMe.value) {
      localStorage.setItem('rememberedUsername', form.username)
      localStorage.setItem('rememberMe', 'true')
    } else {
      localStorage.removeItem('rememberedUsername')
      localStorage.removeItem('rememberMe')
    }

    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const remembered = localStorage.getItem('rememberMe')
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  
  if (remembered === 'true' && rememberedUsername) {
    form.username = rememberedUsername
    rememberMe.value = true
  }
})
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 66px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px 18px;
  background:
    radial-gradient(circle at 12% 14%, rgba(255, 214, 186, 0.72) 0%, rgba(255, 244, 235, 0.18) 28%, transparent 54%),
    linear-gradient(180deg, #fff9f5 0%, #f7f8fb 100%);
}

.login-container {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(380px, 0.94fr);
  max-width: 1080px;
  width: 100%;
  min-height: 680px;
  background: #fff;
  border-radius: 32px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: 0 28px 80px rgba(38, 34, 30, 0.12);
}

.login-left {
  position: relative;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 233, 205, 0.28) 0%, transparent 30%),
    linear-gradient(145deg, #da5324 0%, #e97039 52%, #f3a85c 100%);
  padding: 64px 52px 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.login-left::before {
  content: "";
  position: absolute;
  inset: 28px 34px auto auto;
  width: 164px;
  height: 164px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  opacity: 0.75;
}

.login-left::after {
  content: "";
  position: absolute;
  left: -44px;
  bottom: -56px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.06) 42%, transparent 72%);
}

.login-brand {
  position: relative;
  z-index: 1;
  max-width: 420px;
  text-align: left;
  margin-bottom: 42px;
}

.brand-icon {
  width: 78px;
  height: 78px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 22px;
  backdrop-filter: blur(10px);
}

.login-brand h1 {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 14px;
}

.login-brand p {
  max-width: 320px;
  font-size: 15px;
  line-height: 1.7;
  opacity: 0.92;
}

.login-features {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 18px;
  backdrop-filter: blur(10px);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.24);
  transform: translateX(6px);
}

.feature-item span {
  font-size: 15px;
  font-weight: 500;
}

.login-right {
  position: relative;
  padding: 56px 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 100% 0%, rgba(255, 230, 214, 0.42) 0%, transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #fffaf6 100%);
}

.login-card {
  width: 100%;
  max-width: 410px;
  border: none;
  box-shadow: none;
  background: transparent;
}

.login-card :deep(.el-card__body) {
  padding: 0;
}

.card-header {
  text-align: left;
  margin-bottom: 28px;
}

.card-header h2 {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.card-header p {
  font-size: 15px;
  color: var(--text-secondary);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 50px;
  padding: 0 16px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(217, 182, 157, 0.45) inset;
  background: rgba(255, 255, 255, 0.94);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px rgba(232, 93, 42, 0.58) inset;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  letter-spacing: 0.5px;
}

.demo-users {
  margin-top: 20px;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff8f3 0%, #fff4ed 100%);
  border: 1px solid #f3d7c6;
  box-shadow: 0 14px 32px rgba(233, 126, 69, 0.08);
}

.demo-users__header {
  margin-bottom: 14px;
}

.demo-users__header h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.demo-users__header p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.demo-users__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.demo-user {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 54px;
  padding: 12px 14px;
  margin-bottom: 0;
  border-radius: 16px;
  border: 1px solid #f0d2bf;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.demo-user:hover {
  transform: translateY(-1px);
  border-color: #e85d2a;
  background: #fffaf7;
  box-shadow: 0 10px 24px rgba(232, 93, 42, 0.12);
}

.demo-user__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.register-link {
  text-align: center;
  margin-top: 22px;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-link a {
  color: var(--primary-color);
  font-weight: 500;
}

@media (max-width: 768px) {
  .login-page {
    padding: 20px 12px;
  }

  .login-container {
    grid-template-columns: 1fr;
    min-height: auto;
    border-radius: 26px;
  }

  .login-left {
    padding: 30px 24px 24px;
  }

  .login-right {
    padding: 28px 22px 28px;
  }

  .login-brand {
    margin-bottom: 24px;
  }

  .brand-icon {
    width: 62px;
    height: 62px;
    margin-bottom: 16px;
  }

  .login-brand h1 {
    font-size: 28px;
  }

  .demo-users {
    margin-top: 18px;
  }

  .login-features {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: flex-start;
    gap: 12px;
  }

  .feature-item {
    flex: 1 1 calc(50% - 6px);
    min-width: 0;
    padding: 14px 14px;
  }

  .demo-users__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 430px) {
  .login-left {
    padding: 24px 16px 18px;
  }

  .login-right {
    padding: 20px 16px 22px;
  }

  .card-header {
    margin-bottom: 20px;
  }

  .card-header h2 {
    font-size: 24px;
  }

  .form-options {
    flex-wrap: wrap;
    gap: 8px 14px;
    margin-bottom: 14px;
  }

  .demo-user {
    min-height: 50px;
    padding: 11px 12px;
  }

  .demo-users__grid {
    grid-template-columns: 1fr;
  }

  .feature-item {
    flex: 1 1 100%;
    min-width: 0;
  }

  .login-features {
    display: none;
  }

  .login-brand {
    margin-bottom: 0;
  }
}
</style>
