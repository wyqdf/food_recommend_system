<template>
  <div class="dashboard">
    <el-row :gutter="20" class="dashboard-header">
      <el-col :span="24">
        <h2 class="page-title">数据统计</h2>
        <p class="page-subtitle">聚合展示用户、食谱与互动核心指标</p>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="32">
                <User />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="32">
                <Food />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalRecipes }}</div>
              <div class="stat-label">总食谱数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon :size="32">
                <List />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalCategories }}</div>
              <div class="stat-label">总分类数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: #F56C6C">
              <el-icon :size="32">
                <Comment />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalComments }}</div>
              <div class="stat-label">总评论数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="today-cards">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card small">
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayViews }}</div>
              <div class="stat-label">今日浏览量</div>
            </div>
            <div class="stat-trend" :class="getTrendClass(todayTrend.todayViews)">
              <template v-if="todayTrend.todayViews !== null">
                <el-icon>
                  <Top v-if="todayTrend.todayViews >= 0" />
                  <Bottom v-else />
                </el-icon>
                <span>{{ formatTrend(todayTrend.todayViews) }}</span>
              </template>
              <span v-else>--</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card small">
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayNewUsers }}</div>
              <div class="stat-label">今日新增用户</div>
            </div>
            <div class="stat-trend" :class="getTrendClass(todayTrend.todayNewUsers)">
              <template v-if="todayTrend.todayNewUsers !== null">
                <el-icon>
                  <Top v-if="todayTrend.todayNewUsers >= 0" />
                  <Bottom v-else />
                </el-icon>
                <span>{{ formatTrend(todayTrend.todayNewUsers) }}</span>
              </template>
              <span v-else>--</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-card small">
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayNewRecipes }}</div>
              <div class="stat-label">今日新增食谱</div>
            </div>
            <div class="stat-trend" :class="getTrendClass(todayTrend.todayNewRecipes)">
              <template v-if="todayTrend.todayNewRecipes !== null">
                <el-icon>
                  <Top v-if="todayTrend.todayNewRecipes >= 0" />
                  <Bottom v-else />
                </el-icon>
                <span>{{ formatTrend(todayTrend.todayNewRecipes) }}</span>
              </template>
              <span v-else>--</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>新增用户趋势</span>
            </div>
          </template>
          <div ref="userTrendChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>食谱分类分布</span>
            </div>
          </template>
          <div ref="categoryChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>食谱难度分布</span>
            </div>
          </template>
          <div ref="difficultyChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>热门食谱 TOP 10</span>
            </div>
          </template>
          <el-table :data="topRecipes" style="width: 100%" :height="260">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="title" label="食谱名称" />
            <el-table-column prop="viewCount" label="浏览量" sortable />
            <el-table-column prop="likeCount" label="点赞数" sortable />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-divider content-position="left">
      <span>高级统计</span>
      <el-button type="primary" size="small" :loading="refreshing" style="margin-left: 12px"
        @click="handleRefreshStatistics">
        <el-icon style="margin-right: 4px">
          <Refresh />
        </el-icon>
        {{ refreshing ? '刷新中...' : '手动刷新' }}
      </el-button>
    </el-divider>

    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card quality-card" shadow="hover">
          <div class="stat-card-header">
            <el-icon class="stat-icon" style="color: #67C23A">
              <Star />
            </el-icon>
            <span class="stat-title">高质量食谱</span>
          </div>
          <div class="stat-card-content">
            <div class="stat-number">{{ qualityAnalysis.highQualityRecipes || 0 }}</div>
            <div class="stat-subtitle">
              占比 {{ (qualityAnalysis.qualityRate || 0).toFixed(2) }}%
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-header">
            <el-icon class="stat-icon" style="color: #409EFF">
              <StarFilled />
            </el-icon>
            <span class="stat-title">平均点赞</span>
          </div>
          <div class="stat-card-content">
            <div class="stat-number">{{ Math.floor(qualityAnalysis.averageLikeCount || 0) }}</div>
            <div class="stat-subtitle">次/食谱</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-header">
            <el-icon class="stat-icon" style="color: #E6A23C">
              <ChatDotRound />
            </el-icon>
            <span class="stat-title">平均评论</span>
          </div>
          <div class="stat-card-content">
            <div class="stat-number">{{ Math.floor(qualityAnalysis.averageCommentCount || 0) }}</div>
            <div class="stat-subtitle">条/食谱</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card warning-card" shadow="hover">
          <div class="stat-card-header">
            <el-icon class="stat-icon" style="color: #F56C6C">
              <CircleClose />
            </el-icon>
            <span class="stat-title">零互动食谱</span>
          </div>
          <div class="stat-card-content">
            <div class="stat-number">{{ qualityAnalysis.zeroInteractionRecipes || 0 }}</div>
            <div class="stat-subtitle">需要推广</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Food />
              </el-icon>
              <span>Top 10 食材使用</span>
            </div>
          </template>
          <div ref="ingredientChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Orange />
              </el-icon>
              <span>口味分布</span>
            </div>
          </template>
          <div ref="tasteChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Van />
              </el-icon>
              <span>烹饪技法</span>
            </div>
          </template>
          <div ref="techniqueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <TrendCharts />
              </el-icon>
              <span>互动趋势（近 7 天）</span>
            </div>
          </template>
          <div ref="interactionChartRef" class="chart-container-large"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Medal />
              </el-icon>
              <span>活跃用户 Top 10</span>
            </div>
          </template>
          <div class="user-list-container">
            <div v-for="(user, index) in topActiveUsers" :key="user.userId" class="user-item">
              <div class="user-rank">
                <span v-if="index < 3" class="rank-medal" :class="`medal-${index + 1}`">
                  {{ index + 1 }}
                </span>
                <span v-else class="rank-normal">{{ index + 1 }}</span>
              </div>
              <el-avatar :size="40" class="user-avatar">
                <el-icon>
                  <User />
                </el-icon>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ user.username || `用户${user.userId}` }}</div>
                <div class="user-stats">
                  <el-tag size="small" type="success" effect="plain">
                    <el-icon>
                      <Food />
                    </el-icon>
                    {{ user.recipeCount }}
                  </el-tag>
                  <el-tag size="small" type="primary" effect="plain">
                    <el-icon>
                      <ChatDotRound />
                    </el-icon>
                    {{ user.commentCount }}
                  </el-tag>
                </div>
              </div>
              <div class="user-score">
                <div class="score-value">{{ user.totalScore }}</div>
                <div class="score-label">总分</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <DataLine />
              </el-icon>
              <span>难度分布</span>
            </div>
          </template>
          <div ref="advancedDifficultyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Timer />
              </el-icon>
              <span>耗时分布</span>
            </div>
          </template>
          <div ref="timeCostChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon>
                <Calendar />
              </el-icon>
              <span>月度新增趋势</span>
            </div>
          </template>
          <div ref="monthlyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User, Food, List, Comment, Top, Bottom,
  Star, StarFilled, ChatDotRound, CircleClose, Orange, Van,
  TrendCharts, Medal, DataLine, Timer, Calendar, Refresh
} from '@element-plus/icons-vue'
import { adminStatisticsApi } from '@/api/admin'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer
])

const overview = reactive({
  totalUsers: 0,
  totalRecipes: 0,
  totalCategories: 0,
  totalComments: 0,
  todayViews: 0,
  todayNewUsers: 0,
  todayNewRecipes: 0
})

const todayTrend = reactive({
  todayViews: null,
  todayNewUsers: null,
  todayNewRecipes: null
})

const topRecipes = ref([])
const userTrendChart = ref(null)
const categoryChart = ref(null)
const difficultyChart = ref(null)

const chartInstances = new Map()

const getOrCreateChart = (key, chartRef) => {
  const element = chartRef?.value
  if (!element) return null

  const cached = chartInstances.get(key)
  if (cached && !cached.isDisposed()) {
    return cached
  }

  const existing = echarts.getInstanceByDom(element)
  const chart = existing || echarts.init(element)
  chartInstances.set(key, chart)
  return chart
}

const setChartOption = (key, chartRef, option) => {
  const chart = getOrCreateChart(key, chartRef)
  if (!chart) return
  chart.setOption(option, true)
}

const qualityAnalysis = ref({
  highQualityRecipes: 0,
  averageLikeCount: 0,
  averageCommentCount: 0,
  zeroInteractionRecipes: 0,
  qualityRate: 0
})

const refreshing = ref(false)

const handleRefreshStatistics = async () => {
  refreshing.value = true
  try {
    const res = await adminStatisticsApi.refreshStatistics()
    if (res.code === 200) {
      ElMessage.success(res.data || '刷新成功')
      await loadDashboardData()
    } else {
      ElMessage.error(res.message || '刷新失败')
    }
  } catch (error) {
    console.error('刷新统计数据失败:', error)
    ElMessage.error('刷新失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

const topIngredients = ref([])
const tasteDistribution = ref([])
const techniqueDistribution = ref([])
const interactionTrend = ref([])
const topActiveUsers = ref([])
const difficultyDistribution = ref([])
const timeCostDistribution = ref([])
const monthlyTrend = ref({
  recipeTrend: [],
  userTrend: [],
  commentTrend: []
})

const ingredientChartRef = ref(null)
const tasteChartRef = ref(null)
const techniqueChartRef = ref(null)
const interactionChartRef = ref(null)
const advancedDifficultyChartRef = ref(null)
const timeCostChartRef = ref(null)
const monthlyChartRef = ref(null)

const loadOverview = async () => {
  try {
    const res = await adminStatisticsApi.getOverview()
    if (res.code === 200 && res.data) {
      overview.totalUsers = res.data.totalUsers || 0
      overview.totalRecipes = res.data.totalRecipes || 0
      overview.totalCategories = res.data.totalCategories || 0
      overview.totalComments = res.data.totalComments || 0
      overview.todayViews = res.data.todayViews || 0
      overview.todayNewUsers = res.data.todayNewUsers || 0
      overview.todayNewRecipes = res.data.todayNewRecipes || 0
    }
  } catch (error) {
    console.error('加载概览数据失败:', error)
  }
}

const loadStatistics = async () => {
  try {
    const [userRes, recipeRes] = await Promise.all([
      adminStatisticsApi.getUsers(),
      adminStatisticsApi.getRecipes()
    ])

    if (userRes.code === 200 && userRes.data) {
      const trend = sortTrendByDate(userRes.data.trend || [])
      initUserTrendChart(trend)
      todayTrend.todayNewUsers = calcTrendRate(trend, item => item.count)
    }

    if (recipeRes.code === 200 && recipeRes.data) {
      const recipeTrend = sortTrendByDate(recipeRes.data.trend || [])
      initCategoryChart(recipeRes.data.categoryDistribution || [])
      initDifficultyChart(recipeRes.data.difficultyDistribution || [])
      topRecipes.value = recipeRes.data.topRecipes || []
      todayTrend.todayNewRecipes = calcTrendRate(recipeTrend, item => item.count)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const initUserTrendChart = (data) => {
  const chartData = data && data.length ? data : [
    { date: '暂无数据', count: 0 }
  ]

  setChartOption('userTrend', userTrendChart, {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: chartData.map(item => item.date)
    },
    yAxis: { type: 'value' },
    series: [{
      data: chartData.map(item => item.count),
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409EFF' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.5)' },
          { offset: 1, color: 'rgba(64,158,255,0.05)' }
        ])
      }
    }]
  })
}

const initCategoryChart = (data) => {
  const chartData = data && data.length ? data : [
    { name: '暂无数据', value: 0 }
  ]

  setChartOption('category', categoryChart, {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: chartData.map(item => ({
        name: item.name,
        value: item.value
      })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  })
}

const initDifficultyChart = (data) => {
  const chartData = data && data.length ? data : [
    { name: '暂无数据', value: 0 }
  ]

  setChartOption('difficulty', difficultyChart, {
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: chartData.map(item => item.name)
    },
    yAxis: { type: 'value' },
    series: [{
      data: chartData.map(item => item.value),
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#67C23A' },
          { offset: 1, color: '#95D47A' }
        ])
      },
      barWidth: '40%'
    }]
  })
}

const handleResize = () => {
  chartInstances.forEach(chart => chart.resize())
}

const loadAdvancedStatistics = async () => {
  try {
    const [advancedRes, difficultyRes, timeCostRes, monthlyRes] = await Promise.all([
      adminStatisticsApi.getAdvanced(),
      adminStatisticsApi.getDifficultyDistribution(),
      adminStatisticsApi.getTimeCostDistribution(),
      adminStatisticsApi.getMonthlyTrend()
    ])

    if (advancedRes.code === 200) {
      const data = advancedRes.data
      qualityAnalysis.value = data.qualityAnalysis || {}
      topIngredients.value = data.topIngredients || []
      tasteDistribution.value = data.tasteDistribution || []
      techniqueDistribution.value = data.techniqueDistribution || []
      interactionTrend.value = data.interactionTrend || []
      topActiveUsers.value = data.topActiveUsers || []
      todayTrend.todayViews = calcTrendRate(interactionTrend.value, item => item.viewCount)
    }

    if (difficultyRes.code === 200) {
      difficultyDistribution.value = difficultyRes.data || []
    }

    if (timeCostRes.code === 200) {
      timeCostDistribution.value = timeCostRes.data || []
    }

    if (monthlyRes.code === 200) {
      monthlyTrend.value = monthlyRes.data || {}
    }

    await nextTick()
    renderAdvancedCharts()
  } catch (error) {
    console.error('加载高级统计数据失败:', error)
  }
}

const sortTrendByDate = (trend = []) => {
  return [...trend].sort((a, b) => new Date(a.date) - new Date(b.date))
}

const calcTrendRate = (trend = [], valueGetter) => {
  if (!Array.isArray(trend) || trend.length < 2) return null

  const latest = Number(valueGetter(trend[trend.length - 1])) || 0
  const previous = Number(valueGetter(trend[trend.length - 2])) || 0

  if (previous === 0) {
    return latest === 0 ? 0 : null
  }
  return Number((((latest - previous) / previous) * 100).toFixed(1))
}

const formatTrend = (value) => `${Math.abs(value).toFixed(1)}%`

const getTrendClass = (value) => {
  if (value === null) return ''
  return value >= 0 ? 'up' : 'down'
}

const renderAdvancedCharts = () => {
  renderIngredientChart()
  renderTasteChart()
  renderTechniqueChart()
  renderInteractionChart()
  renderAdvancedDifficultyChart()
  renderTimeCostChart()
  renderMonthlyChart()
}

const renderIngredientChart = () => {
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', name: '食谱数' },
    yAxis: {
      type: 'category',
      data: topIngredients.value.slice(0, 10).map(item => item.ingredientName).reverse()
    },
    series: [{
      name: '食谱数',
      type: 'bar',
      data: topIngredients.value.slice(0, 10).map(item => item.recipeCount).reverse(),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      },
      label: { show: true, position: 'right' }
    }]
  }

  setChartOption('ingredient', ingredientChartRef, option)
}

const renderTasteChart = () => {
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', width: 80 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      labelLine: { show: false },
      data: tasteDistribution.value.map(item => ({ name: item.tasteName, value: item.recipeCount }))
    }]
  }

  setChartOption('taste', tasteChartRef, option)
}

const renderTechniqueChart = () => {
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: techniqueDistribution.value.slice(0, 8).map(item => item.techniqueName),
      axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: { type: 'value', name: '食谱数' },
    series: [{
      name: '食谱数',
      type: 'bar',
      data: techniqueDistribution.value.slice(0, 8).map(item => item.recipeCount),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 1, color: '#188df0' }
        ])
      },
      label: { show: true, position: 'top' }
    }]
  }

  setChartOption('technique', techniqueChartRef, option)
}

const renderInteractionChart = () => {
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['点赞', '收藏', '浏览'] },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: interactionTrend.value.map(item => {
        const date = new Date(item.date)
        return `${date.getMonth() + 1}/${date.getDate()}`
      })
    },
    yAxis: { type: 'value', name: '次数' },
    series: [
      {
        name: '点赞',
        type: 'line',
        data: interactionTrend.value.map(item => item.likeCount || 0),
        smooth: true,
        itemStyle: { color: '#F56C6C' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
            { offset: 1, color: 'rgba(245, 108, 108, 0.01)' }
          ])
        }
      },
      {
        name: '收藏',
        type: 'line',
        data: interactionTrend.value.map(item => item.favoriteCount || 0),
        smooth: true,
        itemStyle: { color: '#E6A23C' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(230, 162, 60, 0.3)' },
            { offset: 1, color: 'rgba(230, 162, 60, 0.01)' }
          ])
        }
      },
      {
        name: '浏览',
        type: 'line',
        data: interactionTrend.value.map(item => item.viewCount || 0),
        smooth: true,
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.01)' }
          ])
        }
      }
    ]
  }

  setChartOption('interaction', interactionChartRef, option)
}

const renderAdvancedDifficultyChart = () => {
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: '70%',
      data: difficultyDistribution.value.map(item => ({
        name: item.techniqueName,
        value: item.recipeCount
      })),
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      },
      itemStyle: { borderRadius: 5 }
    }]
  }

  setChartOption('advancedDifficulty', advancedDifficultyChartRef, option)
}

const renderTimeCostChart = () => {
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: timeCostDistribution.value.map(item => item.techniqueName),
      axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: { type: 'value', name: '食谱数' },
    series: [{
      name: '食谱数',
      type: 'bar',
      data: timeCostDistribution.value.map(item => item.recipeCount),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#37a2da' },
          { offset: 1, color: '#67e0e3' }
        ])
      },
      label: { show: true, position: 'top' }
    }]
  }

  setChartOption('timeCost', timeCostChartRef, option)
}

const renderMonthlyChart = () => {
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['食谱', '用户', '评论'] },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: monthlyTrend.value.recipeTrend?.map(item => item.date) || []
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '食谱',
        type: 'line',
        data: monthlyTrend.value.recipeTrend?.map(item => item.likeCount) || [],
        smooth: true,
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '用户',
        type: 'line',
        data: monthlyTrend.value.userTrend?.map(item => item.likeCount) || [],
        smooth: true,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '评论',
        type: 'line',
        data: monthlyTrend.value.commentTrend?.map(item => item.likeCount) || [],
        smooth: true,
        itemStyle: { color: '#E6A23C' }
      }
    ]
  }

  setChartOption('monthly', monthlyChartRef, option)
}

const loadDashboardData = async () => {
  await Promise.all([
    loadOverview(),
    loadStatistics(),
    loadAdvancedStatistics()
  ])
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await loadDashboardData()
})

onBeforeUnmount(() => {
  chartInstances.forEach(chart => chart.dispose())
  chartInstances.clear()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 26px;
}

.page-subtitle {
  margin-top: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.overview-cards {
  margin-bottom: 20px;
}

.today-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-card.small {
  justify-content: space-between;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
  margin-left: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #67C23A;
}

.stat-trend.up {
  color: #67C23A;
}

.stat-trend.down {
  color: #F56C6C;
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-header {
  margin-bottom: 20px;
}

.mb-4 {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }

  .stat-card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .stat-icon {
      font-size: 24px;
    }

    .stat-title {
      font-size: 14px;
      color: #909399;
      font-weight: 500;
    }
  }

  .stat-card-content {
    .stat-number {
      font-size: 32px;
      font-weight: bold;
      color: #303133;
      line-height: 1;
    }

    .stat-subtitle {
      font-size: 12px;
      color: #909399;
      margin-top: 8px;
    }
  }
}

.quality-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
}

.warning-card {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
}

.chart-card {
  border-radius: 12px;
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 16px;

    .el-icon {
      font-size: 18px;
    }
  }

  .chart-container {
    height: 300px;
    width: 100%;
  }

  .chart-container-large {
    height: 350px;
    width: 100%;
  }
}

.user-list-container {
  max-height: 400px;
  overflow-y: auto;

  .user-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .user-rank {
      width: 40px;
      text-align: center;

      .rank-medal {
        display: inline-block;
        width: 28px;
        height: 28px;
        line-height: 28px;
        border-radius: 50%;
        color: #fff;
        font-weight: bold;
        font-size: 14px;

        &.medal-1 {
          background: linear-gradient(135deg, #FFD700, #FFA500);
          box-shadow: 0 2px 8px rgba(255, 215, 0, 0.4);
        }

        &.medal-2 {
          background: linear-gradient(135deg, #C0C0C0, #A8A8A8);
          box-shadow: 0 2px 8px rgba(192, 192, 192, 0.4);
        }

        &.medal-3 {
          background: linear-gradient(135deg, #CD7F32, #B87333);
          box-shadow: 0 2px 8px rgba(205, 127, 50, 0.4);
        }
      }

      .rank-normal {
        color: #909399;
        font-size: 14px;
      }
    }

    .user-avatar {
      margin: 0 12px;
    }

    .user-info {
      flex: 1;

      .user-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 6px;
      }

      .user-stats {
        display: flex;
        gap: 8px;

        .el-tag {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .user-score {
      text-align: center;

      .score-value {
        font-size: 20px;
        font-weight: bold;
        color: #409EFF;
        line-height: 1;
      }

      .score-label {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

@media (max-width: 768px) {
  .stat-card-content {
    .stat-number {
      font-size: 24px !important;
    }
  }

  .chart-container {
    height: 250px !important;
  }

  .chart-container-large {
    height: 300px !important;
  }
}
</style>
