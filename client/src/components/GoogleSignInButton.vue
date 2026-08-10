<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{ clientId: string }>()
const emit = defineEmits<{ credential: [idToken: string] }>()

const buttonEl = ref<HTMLElement | null>(null)
let pollTimer: ReturnType<typeof setInterval> | undefined

function render() {
  const google = window.google
  if (!google || !buttonEl.value) return

  google.accounts.id.initialize({
    client_id: props.clientId,
    callback: (response) => emit('credential', response.credential),
  })
  google.accounts.id.renderButton(buttonEl.value, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'signin_with',
    shape: 'rectangular',
  })
}

onMounted(() => {
  if (window.google) {
    render()
    return
  }
  // GIS 스크립트는 index.html에서 async로 로드되므로 준비될 때까지 짧게 폴링한다.
  pollTimer = setInterval(() => {
    if (window.google) {
      clearInterval(pollTimer)
      pollTimer = undefined
      render()
    }
  }, 100)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div ref="buttonEl"></div>
</template>
