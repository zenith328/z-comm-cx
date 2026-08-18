<script setup lang="ts">
import { ref, watch } from 'vue'
import { fetchFitProfile } from '../api/clientReviewApi'
import type { FitLevel, FitProfileResponse } from '../types/review'
import { isQuotaExceededError } from '../utils/apiError'

const props = defineProps<{ productCode: string }>()

const profile = ref<FitProfileResponse | null>(null)
const loading = ref(false)
const loadError = ref('')

const FIT_LABELS: Record<FitLevel, string> = {
  TIGHT: '타이트함',
  TRUE_TO_SIZE: '정사이즈',
  LOOSE: '넉넉함',
  UNKNOWN: '정보 부족',
}

function fitClass(level: FitLevel): string {
  return level === 'UNKNOWN' ? 'unknown' : 'known'
}

async function load() {
  loading.value = true
  loadError.value = ''
  profile.value = null
  try {
    profile.value = await fetchFitProfile(props.productCode)
  } catch (error) {
    console.error(error)
    loadError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 핏 가이드를 볼 수 없습니다. 잠시 후 다시 시도해주세요.'
      : '핏 가이드를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

watch(() => props.productCode, load, { immediate: true })
</script>

<template>
  <section v-if="loading || loadError || profile" class="fit-profile-card">
    <h3>AI 핏 가이드</h3>
    <p v-if="loading" class="loading">분석 중...</p>
    <p v-else-if="loadError" class="error">{{ loadError }}</p>
    <template v-else-if="profile">
      <p v-if="profile.fromColdStartFallback" class="fallback-notice">
        아직 리뷰가 충분하지 않아, 실제 고객 후기가 아닌 상품 설명을 바탕으로 추정한 참고용 정보입니다.
      </p>
      <p v-else class="based-on">리뷰 {{ profile.basedOnReviewCount }}건을 참고했습니다.</p>

      <div class="fit-axes">
        <div class="fit-axis">
          <span class="fit-axis-label">{{ profile.axis1Label }}</span>
          <span class="fit-badge" :class="fitClass(profile.shoulderFit)">{{ FIT_LABELS[profile.shoulderFit] }}</span>
        </div>
        <div class="fit-axis">
          <span class="fit-axis-label">{{ profile.axis2Label }}</span>
          <span class="fit-badge" :class="fitClass(profile.chestFit)">{{ FIT_LABELS[profile.chestFit] }}</span>
        </div>
        <div class="fit-axis">
          <span class="fit-axis-label">{{ profile.axis3Label }}</span>
          <span class="fit-badge" :class="fitClass(profile.lengthFit)">{{ FIT_LABELS[profile.lengthFit] }}</span>
        </div>
      </div>

      <p class="recommended-body-type">추천 체형: {{ profile.recommendedBodyType }}</p>
      <p class="summary">{{ profile.summary }}</p>
    </template>
  </section>
</template>

<style scoped>
.fit-profile-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: #f5faff;
}
.fit-profile-card h3 {
  margin: 0 0 10px;
  font-size: 16px;
}
.loading,
.error {
  font-size: 13px;
  margin: 0;
}
.error {
  color: #a80000;
}
.fallback-notice {
  margin: 0 0 12px;
  font-size: 12px;
  color: #8a6500;
  background: #fff3cd;
  border-radius: 4px;
  padding: 6px 10px;
}
.based-on {
  margin: 0 0 12px;
  font-size: 12px;
  color: #888;
}
.fit-axes {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}
.fit-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.fit-axis-label {
  font-size: 12px;
  color: #666;
}
.fit-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 999px;
}
.fit-badge.known {
  background: #dceeff;
  color: #0056b3;
}
.fit-badge.unknown {
  background: #eee;
  color: #999;
}
.recommended-body-type {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.summary {
  margin: 0;
  font-size: 13px;
  color: #444;
  line-height: 1.5;
}
</style>
