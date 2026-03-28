import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api'

const PROFILE_CACHE_TTL_MS = 30 * 1000

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const profileLoadedAt = ref(0)
  let profileRequest = null

  const isLoggedIn = computed(() => !!token.value)

  const login = async (loginData) => {
    const res = await userApi.login(loginData)
    token.value = res.data.token
    user.value = res.data.user
    profileLoadedAt.value = 0
    localStorage.setItem('token', res.data.token)
    await fetchProfile({ force: true })
    return res
  }

  const register = async (registerData) => {
    const res = await userApi.register(registerData)
    return res
  }

  const fetchProfile = async ({ force = false } = {}) => {
    if (!token.value) return null
    const hasFreshProfile = !force
      && user.value
      && profileLoadedAt.value
      && (Date.now() - profileLoadedAt.value) < PROFILE_CACHE_TTL_MS
    if (hasFreshProfile) {
      return user.value
    }
    if (profileRequest) {
      return profileRequest
    }

    profileRequest = (async () => {
      try {
        const res = await userApi.getProfile()
        user.value = res.data
        profileLoadedAt.value = Date.now()
        return user.value
      } catch (e) {
        logout()
        return null
      } finally {
        profileRequest = null
      }
    })()

    return profileRequest
  }

  const logout = () => {
    user.value = null
    token.value = ''
    profileLoadedAt.value = 0
    profileRequest = null
    localStorage.removeItem('token')
  }

  return { user, token, isLoggedIn, login, register, fetchProfile, logout }
})
