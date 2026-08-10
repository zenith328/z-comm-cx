<script setup lang="ts">
import { visibilityLabel } from '../constants/review'

const props = defineProps<{ visible: boolean }>()
</script>

<template>
  <span class="badge" :class="{ visible: props.visible, hidden: !props.visible }">{{
    visibilityLabel(props.visible)
  }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.visible {
  background: #e6f2ff;
  color: #0056b3;
}
.hidden {
  background: #fde7e9;
  color: #a80000;
}
</style>
