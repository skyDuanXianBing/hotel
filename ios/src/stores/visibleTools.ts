import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useVisibleToolsStore = defineStore('visibleTools', () => {
  const memoOpen = ref(false)
  const recordOpen = ref(false)
  const contactOpen = ref(false)

  const openMemo = () => {
    memoOpen.value = true
  }

  const closeMemo = () => {
    memoOpen.value = false
  }

  const openRecord = () => {
    recordOpen.value = true
  }

  const closeRecord = () => {
    recordOpen.value = false
  }

  const openContact = () => {
    contactOpen.value = true
  }

  const closeContact = () => {
    contactOpen.value = false
  }

  return {
    memoOpen,
    recordOpen,
    contactOpen,
    openMemo,
    closeMemo,
    openRecord,
    closeRecord,
    openContact,
    closeContact,
  }
})
