<template>
  <div class="oss-upload">
    <el-upload
      v-model:file-list="fileList"
      class="upload-component"
      :class="{ 'hide-upload': hideUpload }"
      :action="uploadUrl"
      :headers="uploadHeaders"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :limit="limit"
      :accept="accept"
      :drag="drag"
      :multiple="multiple"
      list-type="picture-card"
      :auto-upload="false"
    >
      <el-icon class="upload-icon"><Plus /></el-icon>
    </el-upload>

    <el-dialog v-model="dialogVisible" title="图片预览" width="600px">
      <img :src="dialogImageUrl" alt="预览图片" class="preview-image" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadApi } from '@/api'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  limit: {
    type: Number,
    default: 1
  },
  accept: {
    type: String,
    default: 'image/jpeg,image/png,image/gif,image/webp'
  },
  drag: {
    type: Boolean,
    default: true
  },
  multiple: {
    type: Boolean,
    default: false
  },
  maxSize: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'error'])

const fileList = ref([])
const dialogVisible = ref(false)
const dialogImageUrl = ref('')
const uploading = ref(false)

const uploadUrl = computed(() => '/api/upload/image')

const uploadHeaders = computed(() => ({
}))

const hideUpload = computed(() => {
  return !props.multiple && fileList.value.length >= props.limit
})

const handleBeforeUpload = async (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }

  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }

  uploading.value = true
  return false
}

const handleSuccess = (response, uploadFile) => {
  uploading.value = false
  if (response.code === 200) {
    const url = response.data.url
    if (props.multiple) {
      const urls = fileList.value.map(f => f.url || f.response?.data?.url).filter(Boolean)
      urls.push(url)
      emit('update:modelValue', urls.join(','))
    } else {
      emit('update:modelValue', url)
    }
    emit('success', url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
    emit('error', response.message)
  }
}

const handleError = (error) => {
  uploading.value = false
  ElMessage.error('上传失败: ' + error.message)
  emit('error', error)
}

const handleRemove = (file, fileList) => {
  if (props.multiple) {
    const urls = fileList.map(f => f.url || f.response?.data?.url).filter(Boolean)
    emit('update:modelValue', urls.join(','))
  } else {
    emit('update:modelValue', '')
  }
}

const handlePreview = (file) => {
  dialogImageUrl.value = file.url || file.response?.data?.url
  dialogVisible.value = true
}

const uploadFiles = async () => {
  if (fileList.value.length === 0) return

  const uploadPromises = fileList.value
    .filter(f => !f.url && !f.response?.data?.url)
    .map(async (file) => {
      try {
        const response = await uploadApi.uploadImage(file.raw)
        if (response.code === 200) {
          file.url = response.data.url
          file.status = 'success'
          return response.data.url
        } else {
          file.status = 'fail'
          throw new Error(response.message)
        }
      } catch (error) {
        file.status = 'fail'
        throw error
      }
    })

  const results = await Promise.allSettled(uploadPromises)
  const successUrls = results
    .filter(r => r.status === 'fulfilled')
    .map(r => r.value)
    .filter(Boolean)

  if (props.multiple) {
    emit('update:modelValue', successUrls.join(','))
  } else if (successUrls.length > 0) {
    emit('update:modelValue', successUrls[0])
  }

  emit('success', successUrls)
}

const clearFiles = () => {
  fileList.value = []
  emit('update:modelValue', '')
}

const setImageUrl = (url) => {
  if (url) {
    if (props.multiple) {
      fileList.value = url.split(',').map(u => ({
        url: u,
        name: u.split('/').pop()
      }))
    } else {
      fileList.value = [{
        url: url,
        name: url.split('/').pop()
      }]
    }
  } else {
    fileList.value = []
  }
}

defineExpose({
  uploadFiles,
  clearFiles,
  setImageUrl
})
</script>

<style scoped>
.oss-upload {
  width: 100%;
}

.upload-component :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  line-height: 130px;
}

.upload-component :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 120px;
  height: 120px;
}

.hide-upload :deep(.el-upload--picture-card) {
  display: none;
}

.upload-icon {
  font-size: 28px;
  color: #8c939d;
}

.preview-image {
  width: 100%;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}
</style>
