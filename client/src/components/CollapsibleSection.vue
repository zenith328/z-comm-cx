<script setup lang="ts">
import { ref } from 'vue'

// 시스템관리 화면처럼 기능이 계속 늘어날 수 있는 페이지에서, 각 기능을 접었다 펼 수 있는
// 구분된 영역으로 감싸기 위한 공용 컴포넌트.
const props = withDefaults(defineProps<{ title: string; defaultOpen?: boolean }>(), {
  defaultOpen: true,
})

const open = ref(props.defaultOpen)
</script>

<template>
  <section class="collapsible-section">
    <button type="button" class="collapsible-header" @click="open = !open">
      <span class="collapsible-title">{{ title }}</span>
      <span class="collapsible-icon" :class="{ open }">▾</span>
    </button>
    <div v-if="open" class="collapsible-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.collapsible-section {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
  background: #fff;
}
.collapsible-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 20px;
  border: none;
  background: #f7f9fc;
  cursor: pointer;
  text-align: left;
}
.collapsible-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}
.collapsible-icon {
  color: #888;
  font-size: 14px;
  transition: transform 0.15s ease;
}
.collapsible-icon.open {
  transform: rotate(180deg);
}
.collapsible-body {
  padding: 20px;
  border-top: 1px solid #e0e0e0;
}
</style>
