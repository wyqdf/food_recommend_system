<template>
  <div class="system-logs">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span>系统日志</span>
            <small>管理员操作审计、筛选、详情与清理</small>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="管理员">
          <el-input v-model="searchForm.adminName" placeholder="管理员名称" clearable />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="searchForm.module" placeholder="全部" clearable filterable>
            <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="searchForm.operation" placeholder="全部" clearable filterable>
            <el-option
              v-for="item in operationOptions"
              :key="item"
              :label="formatOperationLabel(item)"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-tag effect="plain" type="info">共 {{ pagination.total }} 条日志</el-tag>
          <el-tag effect="plain">已选 {{ selectedIds.length }} 条</el-tag>
        </div>
        <div class="toolbar-actions">
          <el-button type="danger" plain :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            批量删除
          </el-button>
          <div class="cleanup-box">
            <el-input-number v-model="cleanupDays" :min="1" :max="3650" :step="1" controls-position="right" />
            <el-button type="warning" plain @click="handleCleanup">清理历史日志</el-button>
          </div>
          <el-button link type="primary" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" class="table-full" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="86" />
        <el-table-column prop="adminName" label="管理员" width="120" />
        <el-table-column prop="operation" label="操作类型" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operation)" effect="plain">
              {{ formatOperationLabel(row.operation) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="content" label="操作内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row.id)">详情</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="780px">
      <div v-if="detailData" class="detail-grid">
        <div><b>ID：</b>{{ detailData.id }}</div>
        <div><b>管理员：</b>{{ detailData.adminName }} ({{ detailData.adminId }})</div>
        <div><b>模块：</b>{{ detailData.module }}</div>
        <div><b>操作类型：</b>{{ formatOperationLabel(detailData.operation) }}</div>
        <div><b>目标 ID：</b>{{ detailData.targetId ?? '-' }}</div>
        <div><b>IP：</b>{{ detailData.ip || '-' }}</div>
        <div><b>时间：</b>{{ detailData.createTime }}</div>
        <div><b>User-Agent：</b>{{ detailData.userAgent || '-' }}</div>
      </div>
      <div class="detail-content">
        <b>操作内容：</b>
        <pre>{{ detailData?.content || '-' }}</pre>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminLogApi } from "@/api/admin";

const loading = ref(false);
const detailVisible = ref(false);
const detailData = ref(null);
const selectedIds = ref([]);
const cleanupDays = ref(30);
const moduleOptions = ref([]);
const operationOptions = ref([]);
const tableData = ref([]);
const operationLabelMap = {
  LOGIN: "登录",
  LOGOUT: "退出登录",
  CREATE_USER: "创建用户",
  UPDATE_USER: "更新用户",
  DELETE_USER: "删除用户",
  BATCH_DELETE_USER: "批量删除用户",
  UPDATE_USER_STATUS: "更新用户状态",
  RESET_USER_PASSWORD: "重置用户密码",
  CREATE_RECIPE: "创建食谱",
  UPDATE_RECIPE: "更新食谱",
  DELETE_RECIPE: "删除食谱",
  BATCH_DELETE_RECIPE: "批量删除食谱",
  AUDIT_RECIPE: "审核食谱",
  CREATE_CATEGORY: "创建分类",
  UPDATE_CATEGORY: "更新分类",
  DELETE_CATEGORY: "删除分类",
  CREATE_ATTRIBUTE: "创建属性",
  UPDATE_ATTRIBUTE: "更新属性",
  DELETE_ATTRIBUTE: "删除属性",
  UPDATE_ADMIN_PASSWORD: "修改管理员密码",
  DELETE_LOG: "删除日志",
  BATCH_DELETE_LOG: "批量删除日志",
  CLEANUP_LOG: "清理日志",
  REFRESH_STATISTICS: "刷新统计",
};

const searchForm = reactive({
  adminName: "",
  module: "",
  operation: "",
  dateRange: [],
});

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
});

const buildQueryParams = () => {
  const params = {
    page: pagination.page,
    pageSize: pagination.pageSize,
    adminName: (searchForm.adminName || "").trim() || undefined,
    module: searchForm.module || undefined,
    operation: searchForm.operation || undefined,
  };
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startTime = searchForm.dateRange[0];
    params.endTime = searchForm.dateRange[1];
  }
  return params;
};

const loadMeta = async () => {
  try {
    const res = await adminLogApi.getMeta();
    if (res.code === 200) {
      moduleOptions.value = res.data.modules || [];
      operationOptions.value = res.data.operations || [];
    }
  } catch (error) {
    ElMessage.warning("日志筛选选项加载失败，已使用默认展示");
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await adminLogApi.getList(buildQueryParams());
    if (res.code === 200) {
      tableData.value = res.data.list || [];
      pagination.total = res.data.total || 0;
      selectedIds.value = [];
    }
  } catch (error) {
    ElMessage.error("加载失败：" + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  pagination.page = 1;
  await loadData();
};

const handleReset = async () => {
  searchForm.adminName = "";
  searchForm.module = "";
  searchForm.operation = "";
  searchForm.dateRange = [];
  pagination.page = 1;
  await loadData();
};

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => item.id);
};

const handleViewDetail = async (id) => {
  try {
    const res = await adminLogApi.getById(id);
    if (res.code === 200) {
      detailData.value = res.data;
      detailVisible.value = true;
    }
  } catch (error) {
    ElMessage.error("加载日志详情失败：" + (error.message || "未知错误"));
  }
};

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm("确定删除该日志吗？删除后不可恢复。", "删除确认", {
      type: "warning",
    });
    await adminLogApi.delete(id);
    ElMessage.success("删除成功");
    await loadData();
  } catch (error) {
    if (!isCancelAction(error)) {
      ElMessage.error("删除失败：" + (error.message || "未知错误"));
    }
  }
};

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条日志吗？`, "批量删除确认", {
      type: "warning",
    });
    const res = await adminLogApi.batchDelete(selectedIds.value);
    ElMessage.success(`删除成功，共删除 ${res.data.deletedCount || 0} 条`);
    await loadData();
  } catch (error) {
    if (!isCancelAction(error)) {
      ElMessage.error("批量删除失败：" + (error.message || "未知错误"));
    }
  }
};

const handleCleanup = async () => {
  try {
    await ElMessageBox.confirm(`确定清理 ${cleanupDays.value} 天前的日志吗？`, "清理确认", {
      type: "warning",
    });
    const res = await adminLogApi.cleanup(cleanupDays.value);
    ElMessage.success(`清理完成，共删除 ${res.data.deletedCount || 0} 条`);
    await loadData();
  } catch (error) {
    if (!isCancelAction(error)) {
      ElMessage.error("日志清理失败：" + (error.message || "未知错误"));
    }
  }
};

const isCancelAction = (error) => error === "cancel" || error === "close";

const formatOperationLabel = (operation) => operationLabelMap[operation] || operation || "-";

const getOperationTagType = (operation) => {
  if (!operation) return "info";
  if (operation.includes("DELETE") || operation === "CLEANUP_LOG") return "danger";
  if (operation.includes("UPDATE") || operation.includes("AUDIT")) return "warning";
  if (operation.includes("CREATE") || operation === "LOGIN" || operation === "REFRESH_STATISTICS") return "success";
  return "info";
};

onMounted(async () => {
  await Promise.allSettled([loadMeta(), loadData()]);
});
</script>

<style scoped>
.system-logs {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.search-form {
  margin-bottom: 12px;
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
  gap: 10px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cleanup-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-full {
  width: 100%;
}

.table-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  margin-bottom: 12px;
  font-size: 13px;
}

.detail-content {
  font-size: 13px;
}

.detail-content pre {
  margin-top: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
