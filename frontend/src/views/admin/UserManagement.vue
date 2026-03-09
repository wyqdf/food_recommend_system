<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span>用户管理</span>
            <small>统一维护用户资料、状态与账号安全</small>
          </div>
          <div class="header-actions">
            <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <el-icon><Delete /></el-icon>
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="用户名/昵称/邮箱" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <div class="toolbar-tags">
          <el-tag effect="plain" type="info">总数 {{ pagination.total }}</el-tag>
          <el-tag effect="light" type="warning">已选 {{ selectedIds.length }}</el-tag>
        </div>
        <div class="toolbar-actions">
          <el-button link type="primary" @click="loadData">刷新</el-button>
          <el-button link :disabled="selectedIds.length === 0" @click="clearSelection">清空选择</el-button>
        </div>
      </div>

      <el-table 
        ref="userTableRef"
        :data="tableData" 
        v-loading="loading" 
        class="table-full"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="160" />
        <el-table-column prop="createTime" label="注册时间" min-width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        class="table-pagination"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="状态" v-if="isEdit">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordDialogVisible"
      title="重置密码"
      width="400px"
      @close="handlePasswordDialogClose"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePasswordSubmit" :loading="passwordSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="用户详情"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ currentUser.favoritesCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{ currentUser.commentsCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ currentUser.lastLoginTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ currentUser.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { adminUserApi } from '@/api/admin'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const passwordSubmitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const passwordFormRef = ref(null)
const currentUserId = ref(null)
const currentUser = ref(null)
const selectedIds = ref([])
const userTableRef = ref(null)

const searchForm = reactive({
  keyword: '',
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const formData = reactive({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  password: '',
  status: 1
})

const passwordForm = reactive({
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度在2-50个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const dialogTitle = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminUserApi.getList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    if (res.code === 200) {
      tableData.value = res.data.list
      pagination.total = res.data.total
      selectedIds.value = []
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  pagination.page = 1
  loadData()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const clearSelection = () => {
  userTableRef.value?.clearSelection()
  selectedIds.value = []
}

const handleCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    status: row.status
  })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await adminUserApi.getById(row.id)
    if (res.code === 200) {
      currentUser.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取用户详情失败:', error)
  }
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确定要${newStatus === 1 ? '启用' : '禁用'}该用户吗？`, '提示', {
      type: 'warning'
    })
    await adminUserApi.updateStatus(row.id, { status: newStatus })
    ElMessage.success('操作成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复！', '警告', { type: 'warning' })
    await adminUserApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？此操作不可恢复！`, '警告', { type: 'warning' })
    await adminUserApi.batchDelete(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

const handleResetPassword = (row) => {
  currentUserId.value = row.id
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handlePasswordDialogClose = () => {
  passwordFormRef.value?.resetFields()
}

const handlePasswordSubmit = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        passwordSubmitLoading.value = true
        await adminUserApi.updatePassword(currentUserId.value, {
          password: passwordForm.password
        })
        ElMessage.success('密码重置成功')
        passwordDialogVisible.value = false
      } catch (error) {
        console.error('密码重置失败:', error)
      } finally {
        passwordSubmitLoading.value = false
      }
    }
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          await adminUserApi.update(formData.id, {
            nickname: formData.nickname,
            email: formData.email,
            phone: formData.phone,
            status: formData.status
          })
          ElMessage.success('更新成功')
        } else {
          await adminUserApi.create({
            username: formData.username,
            nickname: formData.nickname,
            email: formData.email,
            phone: formData.phone,
            password: formData.password,
            status: 1
          })
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('操作失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.keys(formData).forEach(key => {
    formData[key] = key === 'status' ? 1 : ''
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-title {
  display: flex;
  flex-direction: column;
}

.header-title span {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
}

.header-title small {
  margin-top: 2px;
  font-size: 12px;
  color: #8a97a8;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 12px;
}

.table-full {
  width: 100%;
}

.table-toolbar {
  margin: 8px 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #edf0f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-tags,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
