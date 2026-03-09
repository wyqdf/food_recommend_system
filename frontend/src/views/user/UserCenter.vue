<template>
  <div class="user-center container page-shell">
    <div class="page-heading">
      <h2 class="title">个人中心</h2>
      <p class="subtitle">管理你的资料与收藏信息</p>
    </div>
    <el-row :gutter="24">
      <el-col :xs="24" :md="7" :lg="6">
        <el-card class="menu-card">
          <div class="user-info">
            <el-avatar :size="64" :src="userStore.user?.avatar">{{ userStore.user?.nickname?.charAt(0) || 'U'
              }}</el-avatar>
            <h3>{{ userStore.user?.nickname || '用户' }}</h3>
          </div>
          <el-menu :default-active="activeMenu" router>
            <el-menu-item index="/user">
              <el-icon>
                <User />
              </el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="/user/favorites">
              <el-icon>
                <Star />
              </el-icon>
              <span>我的收藏</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="17" :lg="18">
        <el-card class="profile-card">
          <template #header>
            <div class="profile-header">
              <span>个人资料</span>
              <el-tag type="success" effect="light">可实时更新</el-tag>
            </div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="头像" prop="avatar">
              <el-input v-model="form.avatar" placeholder="头像URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate" :loading="loading">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Star } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api'

const route = useRoute()
const userStore = useUserStore()
const activeMenu = ref('/user')
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: ''
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const handleUpdate = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userApi.updateProfile({
      nickname: form.nickname,
      email: form.email,
      avatar: form.avatar
    })
    ElMessage.success('更新成功')
    await userStore.fetchProfile()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await userStore.fetchProfile()
  if (userStore.user) {
    form.username = userStore.user.username || ''
    form.nickname = userStore.user.nickname || ''
    form.email = userStore.user.email || ''
    form.avatar = userStore.user.avatar || ''
  }
  activeMenu.value = route.path
})
</script>

<style scoped>
.user-center {
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

.profile-card {
  min-height: 400px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .title {
    font-size: 23px;
  }

  .subtitle {
    font-size: 13px;
  }

  .menu-card {
    min-height: auto;
    margin-bottom: 12px;
  }

  .profile-card {
    min-height: auto;
  }

  .profile-header {
    flex-wrap: wrap;
  }

  :deep(.profile-card .el-form-item__label) {
    width: 100% !important;
    justify-content: flex-start;
    padding-bottom: 4px;
  }

  :deep(.profile-card .el-form-item__content) {
    margin-left: 0 !important;
  }

  :deep(.profile-card .el-button) {
    width: 100%;
    min-height: 40px;
  }
}

@media (max-width: 430px) {
  .title {
    font-size: 21px;
  }

  .user-info {
    padding: 14px 0;
  }
}
</style>
