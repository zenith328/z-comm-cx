<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { fetchVisibleReviews, summarizeReviews } from '../api/clientReviewApi'
import BestReviewShortlist from './BestReviewShortlist.vue'
import ClassificationBadge from './ClassificationBadge.vue'
import FitProfileCard from './FitProfileCard.vue'
import Pagination from './Pagination.vue'
import SentimentBadge from './SentimentBadge.vue'
import { CLASSIFICATION_LABELS, CLASSIFICATION_OPTIONS } from '../constants/review'
import { session } from '../stores/session'
import type { ClientReview, ReviewClassification, ReviewSentiment, ReviewSortOption } from '../types/review'
import { isQuotaExceededError } from '../utils/apiError'

type TypeFilter = 'ALL' | 'TEXT' | 'IMAGE'
type ClassificationFilter = 'ALL' | ReviewClassification
type SentimentFilter = 'ALL' | ReviewSentiment

const PAGE_SIZE = 10

const props = defineProps<{ productCode: string; category: string | null; name: string }>()
defineEmits<{ 'write-review': [] }>()

const reviews = ref<ClientReview[]>([])
const loadingReviews = ref(false)
const reviewsError = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const typeFilter = ref<TypeFilter>('ALL')
const classificationFilter = ref<ClassificationFilter>('ALL')
const sentimentFilter = ref<SentimentFilter>('ALL')
const sortOption = ref<ReviewSortOption>('LATEST')

// 칩으로 바로 보여줄 건 최소한으로 — 나머지(사이즈 팁, 키 구간별 핏)는 선택박스로 옮겨서
// 한 줄에 칩이 너무 많이 늘어서지 않게 한다.
const PINNED_EXAMPLE_QUERIES = [
  { label: '장점 요약', query: '장점만 요약해줘' },
  { label: '단점 요약', query: '단점만 솔직하게 모아줘' },
  { label: '전체 요약', query: '전체적인 내용을 요약해줘' },
]
const SIZE_TIP_QUERY = { label: '사이즈 팁 요약', query: '사이즈 팁만 요약해줘' }

// 키 기반 핏 요약은 상의/하의 같은 의류에만 의미가 있다(신발/가방 등은 키와 사이즈가 상관없음).
// 카테고리에 아래 키워드가 있을 때만 노출한다 — 카테고리가 비어있으면(아직 안 채워진 상품)
// 안전하게 숨긴다.
const CLOTHING_CATEGORY_KEYWORDS = [
  '의류', '상의', '하의', '아우터', '팬츠', '바지', '스커트', '치마', '숏츠', '쇼츠',
  '티셔츠', '블라우스', '니트', '자켓', '재킷', '코트', '점퍼', '셔츠', '원피스', '드레스', '가디건',
]
const HEIGHT_FIT_EXAMPLE_QUERIES = [
  { label: '150cm대 핏 요약', query: '키 148~152cm 정도인 고객이 남긴 사이즈/핏 관련 후기만 모아서 요약해줘' },
  { label: '160cm대 핏 요약', query: '키 158~162cm 정도인 고객이 남긴 사이즈/핏 관련 후기만 모아서 요약해줘' },
  { label: '170cm대 핏 요약', query: '키 168~172cm 정도인 고객이 남긴 사이즈/핏 관련 후기만 모아서 요약해줘' },
  { label: '180cm대 핏 요약', query: '키 178~182cm 정도인 고객이 남긴 사이즈/핏 관련 후기만 모아서 요약해줘' },
]

const isClothingCategory = computed(
  () => !!props.category && CLOTHING_CATEGORY_KEYWORDS.some((keyword) => props.category!.includes(keyword)),
)

/**
 * 카테고리/상품명에 "여성"/"남성"이 명시돼 있으면 그 상품의 타겟 성별로 본다. 어느 쪽도 없으면
 * (유니섹스거나 아직 정보가 부족한 상품) null — 이때는 성별 불일치 근거가 없으니 그냥 보여준다.
 */
function inferProductGender(category: string | null, name: string): 'MALE' | 'FEMALE' | null {
  const text = `${category ?? ''} ${name}`
  if (text.includes('여성')) return 'FEMALE'
  if (text.includes('남성')) return 'MALE'
  return null
}

// 로그인 회원이 "내 정보"에 키/몸무게를 등록해뒀으면, 프리셋 구간 대신 본인 수치를 그대로 넣은
// 맞춤 칩을 하나 더 보여준다 — 프리셋과 똑같이 summarizeReviews를 그대로 재사용할 뿐이라
// 새 백엔드는 필요 없다. 다만 상품이 특정 성별 대상으로 명시돼 있고 내 성별과 다르면(예: 남성이
// 여성 블라우스를 보는 경우), "내 체형"이라는 표현 자체가 안 맞으므로 숨긴다.
const myBodyFitQuery = computed(() => {
  const height = session.current?.heightCm
  const weight = session.current?.weightKg
  const myGender = session.current?.gender
  if (!isClothingCategory.value || height == null || weight == null) return null

  const productGender = inferProductGender(props.category, props.name)
  if (productGender && myGender && productGender !== myGender) return null

  return {
    label: '내 체형 맞춤 핏 요약',
    query: `키 ${height}cm, 몸무게 ${weight}kg 정도인 고객이 남긴 사이즈/핏 관련 후기만 모아서 요약해줘`,
    personalized: true,
  }
})

// 칩(눈에 바로 보이는 것)과 선택박스(더보기용)로 나눈다.
const chipQueries = computed(() => {
  const queries = [...PINNED_EXAMPLE_QUERIES]
  if (myBodyFitQuery.value) queries.push(myBodyFitQuery.value)
  return queries
})
const dropdownQueries = computed(() =>
  isClothingCategory.value ? [SIZE_TIP_QUERY, ...HEIGHT_FIT_EXAMPLE_QUERIES] : [SIZE_TIP_QUERY],
)

function onDropdownSelect(event: Event) {
  const select = event.target as HTMLSelectElement
  const selectedQuery = select.value
  if (!selectedQuery) return
  askExample(selectedQuery)
  select.value = ''
}

const query = ref('')
const summary = ref('')
const summaryReviewCount = ref<number | null>(null)
const summarizing = ref(false)
const summaryError = ref('')

async function fetchReviewList() {
  loadingReviews.value = true
  reviewsError.value = ''
  try {
    const result = await fetchVisibleReviews(props.productCode, {
      page: page.value,
      size: PAGE_SIZE,
      hasPhoto: typeFilter.value === 'ALL' ? undefined : typeFilter.value === 'IMAGE',
      classification: classificationFilter.value === 'ALL' ? undefined : classificationFilter.value,
      sentiment: sentimentFilter.value === 'ALL' ? undefined : sentimentFilter.value,
      sort: sortOption.value,
    })
    reviews.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (error) {
    console.error(error)
    reviewsError.value = '리뷰를 불러오지 못했습니다.'
  } finally {
    loadingReviews.value = false
  }
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value) return
  page.value = target
  fetchReviewList()
}

async function loadReviews() {
  summary.value = ''
  summaryReviewCount.value = null
  query.value = ''
  typeFilter.value = 'ALL'
  classificationFilter.value = 'ALL'
  sentimentFilter.value = 'ALL'
  sortOption.value = 'LATEST'
  page.value = 0
  await fetchReviewList()
}

watch([typeFilter, classificationFilter, sentimentFilter, sortOption], () => {
  page.value = 0
  fetchReviewList()
})

function askExample(text: string) {
  query.value = text
  requestSummary()
}

async function requestSummary() {
  if (!query.value.trim()) return
  summarizing.value = true
  summaryError.value = ''
  try {
    const result = await summarizeReviews(props.productCode, query.value.trim())
    summary.value = result.summary
    summaryReviewCount.value = result.reviewCount
  } catch (error) {
    console.error(error)
    summaryError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과해 지금은 요약을 볼 수 없습니다. 잠시 후 다시 시도해주세요.'
      : '요약을 받아오지 못했습니다.'
  } finally {
    summarizing.value = false
  }
}

function formatDate(value: string): string {
  return value.replace('T', ' ').slice(0, 10)
}

watch(() => props.productCode, loadReviews, { immediate: true })
</script>

<template>
  <section class="review-section">
    <div class="review-section-header">
      <h2>리뷰</h2>
      <button type="button" class="write-button" @click="$emit('write-review')">리뷰 작성하기</button>
    </div>

    <section class="summary-box">
      <h3>AI 리뷰 요약봇</h3>
      <div class="summary-input">
        <input v-model="query" placeholder="궁금한 점을 물어보세요" @keyup.enter="requestSummary" />
        <button type="button" :disabled="summarizing" @click="requestSummary">
          {{ summarizing ? '요약 중...' : '요약 받기' }}
        </button>
      </div>
      <div class="summary-examples">
        <button
          v-for="example in chipQueries"
          :key="example.label"
          type="button"
          class="summary-example"
          :class="{ personalized: example.personalized }"
          :disabled="summarizing"
          @click="askExample(example.query)"
        >
          <span v-if="example.personalized" class="personalized-icon">✨</span>
          {{ example.label }}
        </button>
        <select class="summary-example-select" :disabled="summarizing" @change="onDropdownSelect">
          <option value="">다른 질문 선택...</option>
          <option v-for="example in dropdownQueries" :key="example.label" :value="example.query">
            {{ example.label }}
          </option>
        </select>
      </div>
      <p v-if="summaryError" class="error">{{ summaryError }}</p>
      <p v-else-if="summary" class="summary-result">
        {{ summary }}
        <span class="summary-meta">(참고한 리뷰 {{ summaryReviewCount }}건)</span>
      </p>
    </section>

    <FitProfileCard :product-code="productCode" />

    <BestReviewShortlist :product-code="productCode" />

    <h3 class="review-list-title">리뷰 목록</h3>

    <div class="review-toolbar">
      <div class="review-toolbar-filters">
        <label>
          리뷰타입
          <select v-model="typeFilter">
            <option value="ALL">전체</option>
            <option value="TEXT">텍스트리뷰</option>
            <option value="IMAGE">이미지리뷰</option>
          </select>
        </label>
        <label>
          분류
          <select v-model="classificationFilter">
            <option value="ALL">전체</option>
            <option v-for="option in CLASSIFICATION_OPTIONS" :key="option" :value="option">
              {{ CLASSIFICATION_LABELS[option] }}
            </option>
          </select>
        </label>
        <label>
          감성
          <select v-model="sentimentFilter">
            <option value="ALL">전체</option>
            <option value="POSITIVE">긍정</option>
            <option value="NEUTRAL">중립</option>
            <option value="NEGATIVE">부정</option>
          </select>
        </label>
        <label>
          정렬
          <select v-model="sortOption">
            <option value="LATEST">최신순</option>
            <option value="RATING_HIGH">별점 높은순</option>
            <option value="RATING_LOW">별점 낮은순</option>
            <option value="POSITIVE_FIRST">긍정 우선</option>
            <option value="NEGATIVE_FIRST">부정 우선</option>
          </select>
        </label>
      </div>
      <button type="button" class="refresh-button" :disabled="loadingReviews" @click="fetchReviewList">
        새로고침
      </button>
    </div>

    <p v-if="reviewsError" class="error">{{ reviewsError }}</p>
    <p v-else-if="loadingReviews" class="loading">불러오는 중...</p>
    <p v-else-if="reviews.length === 0" class="empty">등록된 리뷰가 없습니다.</p>

    <template v-else>
      <ul class="review-list">
        <li v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-item-header">
            <span class="rating">★ {{ review.rating }}</span>
            <span class="author">{{ review.memberId }}</span>
            <span v-if="review.hasPhoto" class="photo-tag">사진 첨부</span>
            <ClassificationBadge :classification="review.classification" />
            <SentimentBadge :sentiment="review.sentiment" />
            <span class="date">{{ formatDate(review.createdAt) }}</span>
          </div>
          <p class="content">{{ review.content }}</p>
        </li>
      </ul>

      <Pagination :page="page" :total-pages="totalPages" :total-elements="totalElements" @change="goToPage" />
    </template>
  </section>
</template>

<style scoped>
.review-section {
  border-top: 2px solid #eee;
  padding-top: 20px;
}
.review-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.review-section-header h2 {
  margin: 0;
  font-size: 18px;
}
.write-button {
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #0056b3;
  background: #fff;
  border: 1px solid #0056b3;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
}
.summary-box {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: #fafafa;
}
.summary-box h3 {
  margin: 0 0 10px;
  font-size: 16px;
}
.summary-input {
  display: flex;
  gap: 8px;
}
.summary-input input {
  flex: 1;
  padding: 6px 8px;
  font-size: 13px;
}
.summary-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.summary-example {
  padding: 4px 12px;
  font-size: 12px;
  color: #0056b3;
  background: #eaf2fb;
  border: 1px solid #cfe0f5;
  border-radius: 999px;
  cursor: pointer;
}
.summary-example:hover:not(:disabled) {
  background: #dcebfa;
}
.summary-example.personalized {
  color: #fff;
  background: linear-gradient(135deg, #ff8a3d, #ff5f6d);
  border-color: transparent;
  font-weight: 600;
}
.summary-example.personalized:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff7a28, #ff4b5c);
}
.personalized-icon {
  margin-right: 2px;
}
.summary-example:disabled {
  color: #aaa;
  background: #f2f2f2;
  border-color: #e2e2e2;
  cursor: not-allowed;
}
.summary-example-select {
  padding: 4px 10px;
  font-size: 12px;
  color: #555;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 999px;
  cursor: pointer;
}
.summary-example-select:disabled {
  color: #aaa;
  background: #f2f2f2;
  border-color: #e2e2e2;
  cursor: not-allowed;
}
button {
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
}
.summary-result {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.5;
}
.summary-meta {
  color: #888;
  font-size: 12px;
}
.review-list-title {
  margin: 0 0 12px;
  font-size: 16px;
}
.refresh-button {
  font-size: 13px;
  padding: 5px 12px;
  cursor: pointer;
  flex-shrink: 0;
}
.refresh-button:disabled {
  cursor: default;
  opacity: 0.6;
}
.review-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.review-toolbar-filters {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.review-toolbar-filters label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.review-toolbar-filters select {
  font-size: 13px;
  padding: 3px 6px;
}
.review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.review-item {
  border-bottom: 1px solid #eee;
  padding: 12px 0;
}
.review-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 13px;
}
.rating {
  color: #d68a00;
  font-weight: 600;
}
.author {
  color: #555;
}
.photo-tag {
  color: #0056b3;
  font-size: 12px;
}
.date {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}
.content {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}
.error {
  color: #a80000;
  font-size: 13px;
}
.loading,
.empty {
  color: #666;
  font-size: 13px;
}
</style>
