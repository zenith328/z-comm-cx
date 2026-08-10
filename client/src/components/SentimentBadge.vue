<script setup lang="ts">
import { SENTIMENT_LABELS } from '../constants/review'
import type { ReviewSentiment } from '../types/review'

const props = defineProps<{ sentiment: ReviewSentiment | null }>()
</script>

<template>
  <span v-if="props.sentiment" class="badge" :class="props.sentiment.toLowerCase()">{{
    SENTIMENT_LABELS[props.sentiment]
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
.positive {
  background: #e3f6e6;
  color: #0e6b0e;
}
.neutral {
  background: #eeeeee;
  color: #555555;
}
.negative {
  background: #fde7e9;
  color: #a80000;
}
</style>
