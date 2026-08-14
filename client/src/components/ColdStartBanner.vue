<script setup lang="ts">
import { isWakingUp } from '../stores/coldStart'
</script>

<template>
  <div v-if="isWakingUp" class="cold-start-banner">
    <span class="cold-start-spinner"></span>
    서버를 깨우는 중입니다. 잠시만 기다려 주세요 (최대 1분 정도 걸릴 수 있어요).
  </div>
</template>

<style scoped>
.cold-start-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #fff8e1;
  color: #8a5a00;
  text-align: center;
  padding: 8px 16px;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.cold-start-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #f0dca0;
  border-top-color: #8a5a00;
  border-radius: 50%;
  animation: cold-start-spin 0.8s linear infinite;
}
@keyframes cold-start-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
