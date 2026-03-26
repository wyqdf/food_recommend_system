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
                        <SearchEntry
                            v-model="keyword"
                            source-page="home"
                            size="large"
                            placeholder="搜索菜名、食材、作者..."
                            @submit="handleSearchSubmit"
                        />
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
                                发布我的菜谱
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
import { ref, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { Dish, Bowl, Coffee, ArrowRight, KnifeFork, IceCream, Grid, DocumentAdd } from '@element-plus/icons-vue'
import { recipeApi } from '@/api'
import RecipeGrid from '@/components/RecipeGrid.vue'
import SearchEntry from '@/components/SearchEntry.vue'
import { trackBehavior } from '@/utils/tracker'
import { SEARCH_HOT_KEYWORDS, addSearchHistory } from '@/utils/search'
import { useSceneModeStore } from '@/stores/sceneMode'

const router = useRouter()
const sceneModeStore = useSceneModeStore()
const { currentMode } = storeToRefs(sceneModeStore)
const keyword = ref('')
const categories = ref([])
const recommendList = ref([])
const hotList = ref([])
const loading = ref(false)

const hotTags = SEARCH_HOT_KEYWORDS.slice(0, 5)

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

const handleSearchSubmit = ({ keyword: nextKeyword }) => {
    router.push({ path: '/search', query: { keyword: nextKeyword } })
}

const searchTag = (tag) => {
    addSearchHistory(tag)
    trackBehavior('search_submit', {
        sourcePage: 'home_hot_tag',
        extra: { keyword: tag }
    })
    router.push({ path: '/search', query: { keyword: tag } })
}

const goCategory = (cat) => {
    trackBehavior('category_click', {
        sourcePage: 'home',
        extra: { categoryId: cat.id, categoryName: cat.name }
    })
    router.push({ path: '/recipes', query: { category: cat.id } })
}

const loadData = async () => {
    loading.value = true
    try {
        const [catRes, recRes, hotRes] = await Promise.all([
            recipeApi.getRecommendCategories(8),
            recipeApi.getRecommend({ type: 'personal', limit: 8, mode: currentMode.value }),
            recipeApi.getList({ sort: 'hot', page: 1, pageSize: 8, mode: currentMode.value })
        ])
        categories.value = Array.isArray(catRes.data) ? catRes.data.slice(0, 8) : []
        recommendList.value = recRes.data.list || []
        hotList.value = hotRes.data.list || []
    } catch (error) {
        console.error('加载失败:', error)
    } finally {
        loading.value = false
    }
}

watch(currentMode, async (mode, previousMode) => {
    await loadData()
    trackBehavior('scene_mode_change', {
        sourcePage: 'home',
        extra: { mode, previousMode }
    })
})

onMounted(async () => {
    trackBehavior('page_view', { sourcePage: 'home', extra: { mode: currentMode.value } })
    await loadData()
})
</script>

<style scoped>
.home-page {
    padding-bottom: 46px;
}

.hero-section {
    position: relative;
    text-align: center;
    padding: 64px 34px;
    background: var(--hero-gradient);
    border-radius: var(--radius-lg);
    margin-bottom: 38px;
    overflow: hidden;
    transition: background 0.6s ease, box-shadow var(--transition);
}

.hero-content {
    position: relative;
    z-index: 2;
}

.hero-title {
    font-size: 42px;
    font-weight: 800;
    color: #fff;
    margin-bottom: 12px;
    line-height: 1.3;
}

.title-line {
    display: block;
}

.title-line.highlight {
    color: var(--accent-color);
}

.hero-subtitle {
    font-size: 18px;
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 30px;
}

.hero-search {
    max-width: 600px;
    margin: 0 auto 18px;
}

.hero-search :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.95);
    border-radius: 28px;
    box-shadow: var(--shadow-md);
}

.hero-search :deep(.search-entry__button) {
    border-radius: 28px;
    padding: 0 28px;
    background: var(--accent-color);
    border-color: var(--accent-color);
    color: var(--text-primary);
    font-weight: 600;
}

.hero-search :deep(.search-entry__button:hover) {
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
    margin-top: 18px;
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
    margin-bottom: 38px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 18px;
}

.section-header h2 {
    font-size: 24px;
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
    gap: 14px;
}

.category-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 12px;
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
    width: 56px;
    height: 56px;
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
        font-size: 34px;
    }
}

@media (max-width: 768px) {
    .category-list {
        grid-template-columns: repeat(2, 1fr);
    }

    .hero-section {
        padding: 30px 14px;
        margin-bottom: 22px;
    }

    .hero-title {
        font-size: 24px;
    }

    .hero-search {
        margin-bottom: 12px;
    }

    .hero-search :deep(.search-entry) {
        flex-direction: column;
    }

    .hero-search :deep(.search-entry__button) {
        width: 100%;
        padding: 0 20px;
    }

    .section-header {
        margin-bottom: 12px;
    }

    .section-header h2 {
        font-size: 22px;
    }

    .view-all {
        font-size: 13px;
    }

    .floating-card {
        display: none;
    }
}

@media (max-width: 430px) {
    .home-page {
        padding-bottom: 30px;
    }

    .hero-title {
        font-size: 22px;
    }

    .hero-subtitle {
        font-size: 15px;
        margin-bottom: 14px;
    }

    .hero-tags {
        justify-content: flex-start;
        gap: 8px;
    }

    .tag-label {
        width: 100%;
    }

    .category-item {
        padding: 14px 10px;
    }

    .section-header {
        flex-wrap: wrap;
        gap: 8px;
    }

    .section-header h2 {
        font-size: 20px;
    }

    .category-icon {
        width: 44px;
        height: 44px;
    }
}
</style>
