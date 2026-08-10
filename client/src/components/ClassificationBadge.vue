<script setup lang="ts">
import { CLASSIFICATION_LABELS } from '../constants/review'
import type { ReviewClassification } from '../types/review'

const props = defineProps<{ classification: ReviewClassification }>()
</script>

<template>
  <span class="badge" :class="props.classification.toLowerCase()">{{
    CLASSIFICATION_LABELS[props.classification]
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
.none {
  background: #eeeeee;
  color: #555555;
}
.recommended {
  background: #e6f2ff;
  color: #0056b3;
}
.best_candidate {
  background: #fff0d6;
  color: #a15c00;
}
</style>
