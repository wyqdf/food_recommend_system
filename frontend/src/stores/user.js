import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)

  const login = async (loginData) => {
    const res = await userApi.login(loginData)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    return res
  }

  const register = async (registerData) => {
    const res = await userApi.register(registerData)
    return res
  }

  const fetchProfile = async () => {
    if (!token.value) return
    try {
      const res = await userApi.getProfile()
      user.value = res.data
    } catch (e) {
      logout()
    }
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
  }

  return { user, token, isLoggedIn, login, register, fetchProfile, logout }
})
