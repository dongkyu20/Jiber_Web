import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const loginOpen = ref(false)
  const signupOpen = ref(false)
  const loginRedirect = ref('')

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

  return { loginOpen, signupOpen, loginRedirect, openLogin, openSignup, switchToSignup, switchToLogin, closeAll }
})
