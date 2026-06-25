import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const loginOpen = ref(false)
  const signupOpen = ref(false)
  const loginRedirect = ref('')
  const isDarkMode = ref(readInitialTheme())

  function readInitialTheme() {
    if (typeof window === 'undefined') {
      return true
    }

    return window.localStorage.getItem('jiber-theme') !== 'light'
  }

  function applyTheme() {
    if (typeof document === 'undefined') {
      return
    }

    const theme = isDarkMode.value ? 'dark' : 'light'
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('jiber-theme', theme)
  }

  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
  }

  function openLogin(redirect = '') {
    signupOpen.value = false
    loginRedirect.value = redirect
    loginOpen.value = true
  }

  function openSignup() {
    loginOpen.value = false
    signupOpen.value = true
  }

  function switchToSignup() {
    loginOpen.value = false
    signupOpen.value = true
  }

  function switchToLogin() {
    signupOpen.value = false
    loginOpen.value = true
  }

  function closeAll() {
    loginOpen.value = false
    signupOpen.value = false
    loginRedirect.value = ''
  }

  applyTheme()
  watch(isDarkMode, applyTheme)

  return {
    loginOpen,
    signupOpen,
    loginRedirect,
    isDarkMode,
    toggleDarkMode,
    openLogin,
    openSignup,
    switchToSignup,
    switchToLogin,
    closeAll
  }
})
