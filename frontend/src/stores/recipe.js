import { defineStore } from 'pinia'
import { ref } from 'vue'
import { recipeApi } from '@/api'

export const useRecipeStore = defineStore('recipe', () => {
  const categories = ref([])
  const currentRecipe = ref(null)
  const loading = ref(false)

  const fetchCategories = async () => {
    if (categories.value.length) return
    try {
      const res = await recipeApi.getCategories()
      categories.value = res.data
    } catch (e) {
      console.error('获取分类失败', e)
    }
  }

  const fetchRecipeDetail = async (id) => {
    loading.value = true
    try {
      const res = await recipeApi.getDetail(id)
      currentRecipe.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  return { categories, currentRecipe, loading, fetchCategories, fetchRecipeDetail }
})
