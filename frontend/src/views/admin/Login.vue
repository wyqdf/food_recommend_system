<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="40"><Setting /></el-icon>
        </div>
        <h2 class="login-title">管理后台</h2>
        <p class="login-subtitle">美食推荐系统</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        <el-alert
          title="测试账号"
          description="用户名：admin / 密码：123456"
          type="info"
          :closable="false"
        />
      </div>
      <div class="footer-link">
        <router-link to="/">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await adminApi.login(loginForm)
        if (res.code === 200) {
          localStorage.setItem('admin_token', res.data.token)
          localStorage.setItem('admin_info', JSON.stringify(res.data.admin))
          
          if (loginForm.remember) {
            localStorage.setItem('admin_remembered', loginForm.username)
          } else {
            localStorage.removeItem('admin_remembered')
          }
          
          ElMessage.success('登录成功')
          // 使用 replace: true 避免用户能回退到登录页
          const redirect = route.query.redirect || '/admin/dashboard'
          router.replace(redirect)
        }
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  const remembered = localStorage.getItem('admin_remembered')
  if (remembered) {
    loginForm.username = remembered
    loginForm.remember = true
  }
})
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(circle at 10% 10%, #fff2e9 0%, #f6f8fb 55%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
}

.login-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.login-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.tips {
  margin-top: 20px;
}

.footer-link {
  text-align: center;
  margin-top: 20px;
}

.footer-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
}

.footer-link a:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
}
</style>
