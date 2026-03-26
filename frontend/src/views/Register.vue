<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-left">
        <div class="register-brand">
          <div class="brand-icon">
            <el-icon :size="48">
              <Food />
            </el-icon>
          </div>
          <h1>美食推荐</h1>
          <p>加入我们，开启美食之旅</p>
        </div>
        <div class="register-features">
          <div class="feature-item">
            <el-icon :size="24">
              <User />
            </el-icon>
            <span>个人中心</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24">
              <CollectionTag />
            </el-icon>
            <span>收藏管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24">
              <ChatDotRound />
            </el-icon>
            <span>互动评论</span>
          </div>
        </div>
      </div>
      <div class="register-right">
        <el-card class="register-card">
          <div class="card-header">
            <h2>创建账号</h2>
            <p>填写以下信息完成注册</p>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="form.username" prefix-icon="User" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="form.nickname" prefix-icon="UserFilled" placeholder="请输入昵称" size="large" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="form.email" prefix-icon="Message" placeholder="请输入邮箱（可选）" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" prefix-icon="Lock" placeholder="请输入密码" size="large"
                show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" prefix-icon="Lock" placeholder="请确认密码"
                size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading" size="large">
                注册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="login-link">
            已有账号？<router-link to="/login">立即登录</router-link>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Food, User, CollectionTag, ChatDotRound, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功，首次登录后可选择兴趣标签')
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 66px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 16px;
  background: radial-gradient(circle at 90% 10%, #edfbf8 0%, #f6f8fb 45%);
}

.register-container {
  display: flex;
  max-width: 960px;
  width: 100%;
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.register-left {
  flex: 1;
  background: linear-gradient(135deg, #0f9b8e 0%, #28b5a8 70%, #69ddd2 100%);
  padding: 52px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.register-brand {
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

.register-brand h1 {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 12px;
}

.register-brand p {
  font-size: 14px;
  opacity: 0.9;
}

.register-features {
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

.register-right {
  flex: 1;
  padding: 44px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-card {
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

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: var(--text-secondary);
  font-size: 14px;
}

.login-link a {
  color: var(--primary-color);
  font-weight: 500;
}

@media (max-width: 768px) {
  .register-page {
    padding: 16px 10px;
  }

  .register-container {
    flex-direction: column;
  }

  .register-left {
    padding: 24px 18px;
  }

  .register-right {
    padding: 20px 18px 24px;
  }

  .register-brand {
    margin-bottom: 20px;
  }

  .brand-icon {
    width: 62px;
    height: 62px;
    margin-bottom: 14px;
  }

  .register-brand h1 {
    font-size: 24px;
  }

  .register-features {
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
  .register-left {
    padding: 24px 14px 20px;
  }

  .register-right {
    padding: 14px 12px 20px;
  }

  .card-header h2 {
    font-size: 24px;
  }

  .feature-item {
    flex: 1 1 100%;
    min-width: 0;
  }

  .register-features {
    display: none;
  }

  .register-brand {
    margin-bottom: 0;
  }
}
</style>

