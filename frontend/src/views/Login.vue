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
          <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" prefix-icon="User" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" prefix-icon="Lock" placeholder="请输入密码" size="large"
                show-password />
            </el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-checkbox v-model="autoLogin">自动登录</el-checkbox>
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
              <p>以下 5 个账号用于演示 Top100 推荐效果，密码统一为 <strong>123456</strong></p>
            </div>
            <button
              v-for="user in demoUsers"
              :key="user.username"
              type="button"
              class="demo-user"
              @click="fillDemoUser(user.username)"
            >
              <span class="demo-user__name">{{ user.username }}</span>
              <span class="demo-user__style">{{ user.style }}</span>
            </button>
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
const autoLogin = ref(false)
const demoUsers = [
  { username: '好吃好吃难做', style: '减脂一人食' },
  { username: '梅依旧', style: '快手家常' },
  { username: '千惠烘焙', style: '烘焙甜点' },
  { username: '小红马', style: '儿童辅食' },
  { username: 'littlelittle', style: '早餐面点' }
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
    
    if (autoLogin.value) {
      localStorage.setItem('autoLogin', 'true')
    } else {
      localStorage.removeItem('autoLogin')
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
  
  const autoLoginFlag = localStorage.getItem('autoLogin')
  if (autoLoginFlag === 'true') {
    autoLogin.value = true
  }
})
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 66px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 16px;
  background: radial-gradient(circle at 10% 10%, #fff4eb 0%, #f6f8fb 45%);
}

.login-container {
  display: flex;
  max-width: 960px;
  width: 100%;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #e85d2a 0%, #f08b5d 70%, #ffb347 100%);
  padding: 52px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.login-brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand-icon {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
}

.login-brand h1 {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 12px;
}

.login-brand p {
  font-size: 14px;
  opacity: 0.9;
}

.login-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(8px);
}

.feature-item span {
  font-size: 15px;
}

.login-right {
  flex: 1;
  padding: 46px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 360px;
  border: none;
  box-shadow: none;
}

.card-header {
  text-align: center;
  margin-bottom: 24px;
}

.card-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.card-header p {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

.demo-users {
  margin-top: 18px;
  padding: 16px;
  border-radius: 16px;
  background: #fff7f1;
  border: 1px solid #f6d8c6;
}

.demo-users__header {
  margin-bottom: 12px;
}

.demo-users__header h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.demo-users__header p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.demo-user {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 12px;
  border: 1px solid #f0d2bf;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.demo-user:last-child {
  margin-bottom: 0;
}

.demo-user:hover {
  transform: translateY(-1px);
  border-color: #e85d2a;
  box-shadow: 0 8px 20px rgba(232, 93, 42, 0.12);
}

.demo-user__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.demo-user__style {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff0e8;
  color: #d65523;
  font-size: 12px;
  font-weight: 600;
}

.form-options {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-link a {
  color: var(--primary-color);
  font-weight: 500;
}

@media (max-width: 768px) {
  .login-page {
    padding: 16px 10px;
  }

  .login-container {
    flex-direction: column;
  }

  .login-left {
    padding: 24px 18px;
  }

  .login-right {
    padding: 20px 18px 24px;
  }

  .login-brand {
    margin-bottom: 20px;
  }

  .brand-icon {
    width: 62px;
    height: 62px;
    margin-bottom: 14px;
  }

  .login-brand h1 {
    font-size: 24px;
  }

  .demo-users {
    margin-top: 16px;
  }

  .login-features {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
  }

  .feature-item {
    padding: 12px 16px;
    min-width: 130px;
  }
}

@media (max-width: 430px) {
  .login-left {
    padding: 24px 14px 20px;
  }

  .login-right {
    padding: 14px 12px 20px;
  }

  .card-header {
    margin-bottom: 20px;
  }

  .card-header h2 {
    font-size: 24px;
  }

  .form-options {
    flex-wrap: wrap;
    gap: 8px 16px;
    margin-bottom: 14px;
  }

  .demo-user {
    align-items: flex-start;
    flex-direction: column;
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
