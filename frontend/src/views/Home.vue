<template>
    <div class="home-page">
        <div class="container">
            <section class="hero-section">
                <div class="hero-content">
                    <h1 class="hero-title">
                        <span class="title-line">发现美食</span>
                        <span class="title-line highlight">享受烹饪</span>
                    </h1>
                    <p class="hero-subtitle">海量菜谱，智能推荐，让每一餐都精彩</p>
                    <div class="hero-search">
                        <el-input v-model="keyword" placeholder="搜索你想要的菜谱..." size="large" @keyup.enter="handleSearch">
                            <template #prefix>
                                <el-icon>
                                    <Search />
                                </el-icon>
                            </template>
                        </el-input>
                        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
                    </div>
                    <div class="hero-tags">
                        <span class="tag-label">热门搜索：</span>
                        <el-tag v-for="tag in hotTags" :key="tag" class="hot-tag" @click="searchTag(tag)">{{ tag
                            }}</el-tag>
                    </div>
                    <div class="hero-actions">
                        <router-link to="/create-recipe">
                            <el-button type="success" size="large">
                                <el-icon>
                                    <DocumentAdd />
                                </el-icon>
                                创建我的菜谱
                            </el-button>
                        </router-link>
                    </div>
                </div>
                <div class="hero-decoration">
                    <div class="floating-card card-1">
                        <el-icon :size="32">
                            <Dish />
                        </el-icon>
                    </div>
                    <div class="floating-card card-2">
                        <el-icon :size="28">
                            <Bowl />
                        </el-icon>
                    </div>
                    <div class="floating-card card-3">
                        <el-icon :size="24">
                            <Coffee />
                        </el-icon>
                    </div>
                </div>
            </section>

            <section class="category-section">
                <div class="section-header">
                    <h2>热门分类</h2>
                    <router-link to="/recipes" class="view-all">查看全部 <el-icon>
                            <ArrowRight />
                        </el-icon></router-link>
                </div>
                <div class="category-list">
                    <div v-for="(cat, index) in categories" :key="cat.id" class="category-item" @click="goCategory(cat)"
                        :style="{ animationDelay: `${index * 0.05}s` }">
                        <div class="category-icon" :style="{ background: getCategoryColor(index) }">
                            <el-icon :size="28">
                                <component :is="getCategoryIcon(index)" />
                            </el-icon>
                        </div>
                        <span class="category-name">{{ cat.name }}</span>
                    </div>
                </div>
            </section>

            <section class="recommend-section">
                <div class="section-header">
                    <h2>为你推荐</h2>
                    <router-link to="/recommend" class="view-all">更多推荐 <el-icon>
                            <ArrowRight />
                        </el-icon></router-link>
                </div>
                <RecipeGrid :recipes="recommendList" :loading="loading" />
            </section>

            <section class="hot-section">
                <div class="section-header">
                    <h2>热门菜谱</h2>
                    <router-link to="/recipes?sort=hot" class="view-all">查看全部 <el-icon>
                            <ArrowRight />
                        </el-icon></router-link>
                </div>
                <RecipeGrid :recipes="hotList" :loading="loading" />
            </section>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Dish, Bowl, Coffee, ArrowRight, KnifeFork, IceCream, Grid, DocumentAdd } from '@element-plus/icons-vue'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'

const router = useRouter()
const keyword = ref('')
const categories = ref([])
const recommendList = ref([])
const hotList = ref([])
const loading = ref(false)

const hotTags = ['红烧肉', '可乐鸡翅', '糖醋排骨', '宫保鸡丁', '麻婆豆腐']

const categoryIcons = [KnifeFork, Bowl, Coffee, IceCream, Dish, Grid, KnifeFork, Bowl]
const categoryColors = [
    'linear-gradient(135deg, #ff6b6b, #ff8e8e)',
    'linear-gradient(135deg, #4ecdc4, #6ee7de)',
    'linear-gradient(135deg, #ffe66d, #fff59d)',
    'linear-gradient(135deg, #a29bfe, #c4b5fd)',
    'linear-gradient(135deg, #fd79a8, #f8a5c2)',
    'linear-gradient(135deg, #00b894, #55efc4)',
    'linear-gradient(135deg, #e17055, #fab1a0)',
    'linear-gradient(135deg, #74b9ff, #a8d8ff)'
]

const getCategoryIcon = (index) => categoryIcons[index % categoryIcons.length]
const getCategoryColor = (index) => categoryColors[index % categoryColors.length]

const handleSearch = () => {
    if (keyword.value.trim()) {
        router.push({ path: '/search', query: { keyword: keyword.value } })
    }
}

const searchTag = (tag) => {
    router.push({ path: '/search', query: { keyword: tag } })
}

const goCategory = (cat) => {
    router.push({ path: '/recipes', query: { category: cat.id } })
}

const loadData = async () => {
    const cachedCategories = sessionStorage.getItem('categories')
    const cachedRecommend = sessionStorage.getItem('recommendList')
    const cachedHot = sessionStorage.getItem('hotList')

    if (cachedCategories && cachedRecommend && cachedHot) {
        categories.value = JSON.parse(cachedCategories)
        recommendList.value = JSON.parse(cachedRecommend)
        hotList.value = JSON.parse(cachedHot)
        return
    }

    loading.value = true
    try {
        const [catRes, recRes, hotRes] = await Promise.all([
            recipeApi.getCategories(),
            recipeApi.getRecommend({ type: 'personal', limit: 8 }),
            recipeApi.getList({ sort: 'hot', pageSize: 8 })
        ])
        categories.value = catRes.data.slice(0, 8)
        recommendList.value = recRes.data.list || []
        hotList.value = hotRes.data.list || []

        sessionStorage.setItem('categories', JSON.stringify(categories.value))
        sessionStorage.setItem('recommendList', JSON.stringify(recommendList.value))
        sessionStorage.setItem('hotList', JSON.stringify(hotList.value))
        sessionStorage.setItem('cacheTime', Date.now().toString())
    } catch (error) {
        console.error('加载失败:', error)
    } finally {
        loading.value = false
    }
}

onMounted(async () => {
    const cacheTime = sessionStorage.getItem('cacheTime')
    if (cacheTime && (Date.now() - parseInt(cacheTime)) < 3600000) {
        categories.value = JSON.parse(sessionStorage.getItem('categories'))
        recommendList.value = JSON.parse(sessionStorage.getItem('recommendList'))
        hotList.value = JSON.parse(sessionStorage.getItem('hotList'))
    } else {
        await loadData()
    }
})
</script>

<style scoped>
.home-page {
    padding-bottom: 60px;
}

.hero-section {
    position: relative;
    text-align: center;
    padding: 80px 40px;
    background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 50%, #ffa8a8 100%);
    border-radius: var(--radius-lg);
    margin-bottom: 48px;
    overflow: hidden;
}

.hero-content {
    position: relative;
    z-index: 2;
}

.hero-title {
    font-size: 48px;
    font-weight: 800;
    color: #fff;
    margin-bottom: 16px;
    line-height: 1.3;
}

.title-line {
    display: block;
}

.title-line.highlight {
    color: var(--accent-color);
}

.hero-subtitle {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 40px;
}

.hero-search {
    display: flex;
    justify-content: center;
    gap: 12px;
    max-width: 560px;
    margin: 0 auto 24px;
}

.hero-search .el-input {
    flex: 1;
}

.hero-search :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.95);
    border-radius: 28px;
    box-shadow: var(--shadow-md);
}

.hero-search .el-button {
    border-radius: 28px;
    padding: 0 32px;
    background: var(--accent-color);
    border-color: var(--accent-color);
    color: var(--text-primary);
    font-weight: 600;
}

.hero-search .el-button:hover {
    background: #ffd93d;
    border-color: #ffd93d;
}

.hero-tags {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.hero-actions {
    margin-top: 20px;
}

.tag-label {
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
}

.hot-tag {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    color: #fff;
    cursor: pointer;
    transition: var(--transition);
}

.hot-tag:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(1.05);
}

.hero-decoration {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
}

.floating-card {
    position: absolute;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(10px);
    border-radius: var(--radius-md);
    padding: 20px;
    color: #fff;
    animation: float 6s ease-in-out infinite;
}

.card-1 {
    top: 20%;
    left: 8%;
    animation-delay: 0s;
}

.card-2 {
    top: 60%;
    right: 10%;
    animation-delay: 2s;
}

.card-3 {
    bottom: 20%;
    left: 15%;
    animation-delay: 4s;
}

@keyframes float {

    0%,
    100% {
        transform: translateY(0);
    }

    50% {
        transform: translateY(-20px);
    }
}

.category-section,
.recommend-section,
.hot-section {
    margin-bottom: 48px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

.section-header h2 {
    font-size: 26px;
    font-weight: 700;
    color: var(--text-primary);
    position: relative;
    padding-left: 16px;
}

.section-header h2::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 24px;
    background: var(--primary-color);
    border-radius: 2px;
}

.view-all {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--text-secondary);
    font-size: 14px;
    transition: var(--transition);
}

.view-all:hover {
    color: var(--primary-color);
}

.category-list {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 16px;
}

.category-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24px 16px;
    background: #fff;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: var(--transition);
    animation: fadeInUp 0.5s ease forwards;
    opacity: 0;
}

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.category-item:hover {
    transform: translateY(-8px);
    box-shadow: var(--shadow-lg);
}

.category-icon {
    width: 60px;
    height: 60px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    margin-bottom: 12px;
}

.category-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
}

@media (max-width: 1024px) {
    .category-list {
        grid-template-columns: repeat(4, 1fr);
    }

    .hero-title {
        font-size: 36px;
    }
}

@media (max-width: 768px) {
    .category-list {
        grid-template-columns: repeat(2, 1fr);
    }

    .hero-section {
        padding: 40px 20px;
    }

    .hero-title {
        font-size: 28px;
    }

    .hero-search {
        flex-direction: column;
    }

    .floating-card {
        display: none;
    }
}
</style>
