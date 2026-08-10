<script setup lang="ts">
import { STATUS_LABELS } from '../constants/review'
import type { ReviewStatus } from '../types/review'

const props = defineProps<{ status: ReviewStatus }>()
</script>

<template>
  <span class="badge" :class="props.status.toLowerCase()">{{ STATUS_LABELS[props.status] }}</span>
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
.pending_ai {
  background: #fff4ce;
  color: #8a6d00;
}
.analyzed {
  background: #dff6dd;
  color: #0e6b0e;
}
.failed {
  background: #fde7e9;
  color: #a80000;
}
</style>
