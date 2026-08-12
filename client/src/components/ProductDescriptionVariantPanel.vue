<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchProduct, updateProductDescription } from '../api/products'
import {
  approveProductDescriptionVariant,
  deleteProductDescriptionVariant,
  editProductDescriptionVariant,
  extractDescriptionFromUrl,
  fetchProductDescriptionVariants,
  generateAllProductDescriptionVariants,
  generateProductDescriptionVariant,
} from '../api/productDescriptionVariants'
import type { CustomerSegment, ProductDescriptionVariantResponse } from '../api/cs-types'
import { isQuotaExceededError } from '../utils/apiError'

const props = defineProps<{ productId: number }>()

const loading = ref(false)
const loadError = ref('')

const baseDescription = ref('')
const baseDescriptionSaved = ref('')
const savingBase = ref(false)
const baseSaveError = ref('')

const extractUrl = ref('')
const extracting = ref(false)
const extractError = ref('')

const variants = ref<ProductDescriptionVariantResponse[]>([])
const generatingSegment = ref<CustomerSegment | null>(null)
const approvingSegment = ref<CustomerSegment | null>(null)
const actionErrorSegment = ref<CustomerSegment | null>(null)
const actionErrorMessage = ref('처리 실패')
const generatingAll = ref(false)
const generateAllError = ref('')

const editingSegment = ref<CustomerSegment | null>(null)
const editDrafts = ref<Record<string, string>>({})
const savingEdit = ref(false)
const deletingSegment = ref<CustomerSegment | null>(null)

const anyBusy = computed(
  () =>
    generatingAll.value ||
    generatingSegment.value !== null ||
    approvingSegment.value !== null ||
    savingEdit.value ||
    deletingSegment.value !== null,
)

const maleVariants = () => variants.value.filter((v) => v.segment.startsWith('MALE'))
const femaleVariants = () => variants.value.filter((v) => v.segment.startsWith('FEMALE'))

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [product, variantList] = await Promise.all([
      fetchProduct(props.productId),
      fetchProductDescriptionVariants(props.productId),
    ])
    baseDescription.value = product.description ?? ''
    baseDescriptionSaved.value = product.description ?? ''
    variants.value = variantList
  } catch (error) {
    console.error(error)
    loadError.value = '상세설명 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function saveBaseDescription() {
  savingBase.value = true
  baseSaveError.value = ''
  try {
    const updated = await updateProductDescription(props.productId, baseDescription.value)
    baseDescriptionSaved.value = updated.description ?? ''
  } catch (error) {
    console.error(error)
    baseSaveError.value = '기본 상세설명 저장에 실패했습니다.'
  } finally {
    savingBase.value = false
  }
}

async function extractFromUrl() {
  if (!extractUrl.value.trim()) return
  extracting.value = true
  extractError.value = ''
  try {
    baseDescription.value = await extractDescriptionFromUrl(props.productId, extractUrl.value.trim())
  } catch (error) {
    console.error(error)
    extractError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '텍스트를 추출하지 못했습니다. URL을 확인해주세요.'
  } finally {
    extracting.value = false
  }
}

async function generate(segment: CustomerSegment) {
  generatingSegment.value = segment
  actionErrorSegment.value = null
  try {
    const updated = await generateProductDescriptionVariant(props.productId, segment)
    applyUpdated(updated)
  } catch (error) {
    console.error(error)
    actionErrorSegment.value = segment
    actionErrorMessage.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '처리 실패'
  } finally {
    generatingSegment.value = null
  }
}

async function approve(segment: CustomerSegment) {
  approvingSegment.value = segment
  actionErrorSegment.value = null
  try {
    const updated = await approveProductDescriptionVariant(props.productId, segment)
    applyUpdated(updated)
  } catch (error) {
    console.error(error)
    actionErrorSegment.value = segment
    actionErrorMessage.value = '처리 실패'
  } finally {
    approvingSegment.value = null
  }
}

function applyUpdated(updated: ProductDescriptionVariantResponse) {
  const index = variants.value.findIndex((v) => v.segment === updated.segment)
  if (index !== -1) variants.value[index] = updated
}

function startEdit(row: ProductDescriptionVariantResponse) {
  editingSegment.value = row.segment
  editDrafts.value[row.segment] = row.content ?? ''
}

function cancelEdit() {
  editingSegment.value = null
}

async function saveEdit(segment: CustomerSegment) {
  savingEdit.value = true
  actionErrorSegment.value = null
  try {
    const updated = await editProductDescriptionVariant(props.productId, segment, editDrafts.value[segment] ?? '')
    applyUpdated(updated)
    editingSegment.value = null
  } catch (error) {
    console.error(error)
    actionErrorSegment.value = segment
    actionErrorMessage.value = '처리 실패'
  } finally {
    savingEdit.value = false
  }
}

async function removeVariant(segment: CustomerSegment) {
  if (!window.confirm('이 세그먼트의 설명을 삭제하고 미생성 상태로 되돌릴까요?')) return
  deletingSegment.value = segment
  actionErrorSegment.value = null
  try {
    const updated = await deleteProductDescriptionVariant(props.productId, segment)
    applyUpdated(updated)
  } catch (error) {
    console.error(error)
    actionErrorSegment.value = segment
    actionErrorMessage.value = '처리 실패'
  } finally {
    deletingSegment.value = null
  }
}

async function generateAll() {
  generatingAll.value = true
  generateAllError.value = ''
  try {
    variants.value = await generateAllProductDescriptionVariants(props.productId)
  } catch (error) {
    console.error(error)
    generateAllError.value = isQuotaExceededError(error)
      ? 'AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
      : '전체 생성에 실패했습니다. 기본 상세설명이 입력되어 있는지 확인해주세요.'
  } finally {
    generatingAll.value = false
  }
}

function statusLabel(status: ProductDescriptionVariantResponse['status']): string {
  if (status === 'APPROVED') return '승인됨'
  if (status === 'DRAFT') return '초안(미승인)'
  return '미생성'
}

const isBaseDirty = () => baseDescription.value !== baseDescriptionSaved.value

onMounted(load)
</script>

<template>
  <div class="description-panel">
    <p v-if="loading" class="loading">불러오는 중...</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>

    <template v-if="!loading && !loadError">
      <div class="base-description">
        <h4>기본 상품 상세설명</h4>
        <div class="image-extract-bar">
          <input
            v-model="extractUrl"
            type="text"
            placeholder="상품 설명 페이지 URL 또는 이미지 URL을 붙여넣으세요"
            class="image-url-input"
          />
          <button type="button" :disabled="!extractUrl.trim() || extracting" @click="extractFromUrl">
            {{ extracting ? '추출 중...' : 'URL에서 텍스트 추출' }}
          </button>
        </div>
        <p v-if="extractError" class="error">{{ extractError }}</p>
        <textarea v-model="baseDescription" rows="4" placeholder="상품의 기본 상세설명을 입력하세요."></textarea>
        <div class="base-description-footer">
          <span v-if="baseSaveError" class="error">{{ baseSaveError }}</span>
          <button type="button" :disabled="!isBaseDirty() || savingBase" @click="saveBaseDescription">
            {{ savingBase ? '저장 중...' : '기본 설명 저장' }}
          </button>
        </div>
      </div>

      <p class="hint">
        세그먼트별 "AI 생성"은 기본 설명 + "성향키워드" 메뉴에 등록된 (전체 상품 공통) 키워드를 함께 참고해서 작성됩니다.
        생성 결과는 검수 후 "승인"을 눌러야 노출 대상이 됩니다.
      </p>

      <div class="generate-all-bar">
        <span v-if="generateAllError" class="error">{{ generateAllError }}</span>
        <button type="button" class="generate-all-button" :disabled="anyBusy" @click="generateAll">
          {{ generatingAll ? '전체 생성 중...' : '전체 생성 (6개 세그먼트 한번에)' }}
        </button>
      </div>

      <div class="variant-groups">
        <div class="variant-group">
          <h4>남성</h4>
          <div v-for="row in maleVariants()" :key="row.segment" class="variant-row">
            <div class="variant-row-header">
              <span class="segment-label">{{ row.segmentLabel }}</span>
              <span class="status-badge" :class="row.status.toLowerCase()">{{ statusLabel(row.status) }}</span>
            </div>
            <template v-if="editingSegment === row.segment">
              <textarea v-model="editDrafts[row.segment]" rows="3" class="edit-textarea"></textarea>
              <div class="variant-row-footer">
                <span v-if="actionErrorSegment === row.segment" class="error">{{ actionErrorMessage }}</span>
                <button type="button" :disabled="savingEdit" @click="saveEdit(row.segment)">
                  {{ savingEdit ? '저장 중...' : '저장' }}
                </button>
                <button type="button" :disabled="savingEdit" @click="cancelEdit">취소</button>
              </div>
            </template>
            <template v-else>
              <p v-if="row.content" class="variant-content">{{ row.content }}</p>
              <p v-else class="variant-content empty">아직 생성된 설명이 없습니다.</p>
              <div class="variant-row-footer">
                <span v-if="actionErrorSegment === row.segment" class="error">{{ actionErrorMessage }}</span>
                <button type="button" :disabled="anyBusy" @click="generate(row.segment)">
                  {{ generatingSegment === row.segment ? '생성 중...' : row.status === 'NOT_GENERATED' ? 'AI 생성' : 'AI 재생성' }}
                </button>
                <button
                  type="button"
                  :disabled="row.status !== 'DRAFT' || anyBusy"
                  @click="approve(row.segment)"
                >
                  {{ approvingSegment === row.segment ? '승인 중...' : '승인' }}
                </button>
                <button type="button" :disabled="anyBusy" @click="startEdit(row)">수정</button>
                <button
                  type="button"
                  :disabled="row.status === 'NOT_GENERATED' || anyBusy"
                  @click="removeVariant(row.segment)"
                >
                  {{ deletingSegment === row.segment ? '삭제 중...' : '삭제' }}
                </button>
              </div>
            </template>
          </div>
        </div>

        <div class="variant-group">
          <h4>여성</h4>
          <div v-for="row in femaleVariants()" :key="row.segment" class="variant-row">
            <div class="variant-row-header">
              <span class="segment-label">{{ row.segmentLabel }}</span>
              <span class="status-badge" :class="row.status.toLowerCase()">{{ statusLabel(row.status) }}</span>
            </div>
            <template v-if="editingSegment === row.segment">
              <textarea v-model="editDrafts[row.segment]" rows="3" class="edit-textarea"></textarea>
              <div class="variant-row-footer">
                <span v-if="actionErrorSegment === row.segment" class="error">{{ actionErrorMessage }}</span>
                <button type="button" :disabled="savingEdit" @click="saveEdit(row.segment)">
                  {{ savingEdit ? '저장 중...' : '저장' }}
                </button>
                <button type="button" :disabled="savingEdit" @click="cancelEdit">취소</button>
              </div>
            </template>
            <template v-else>
              <p v-if="row.content" class="variant-content">{{ row.content }}</p>
              <p v-else class="variant-content empty">아직 생성된 설명이 없습니다.</p>
              <div class="variant-row-footer">
                <span v-if="actionErrorSegment === row.segment" class="error">{{ actionErrorMessage }}</span>
                <button type="button" :disabled="anyBusy" @click="generate(row.segment)">
                  {{ generatingSegment === row.segment ? '생성 중...' : row.status === 'NOT_GENERATED' ? 'AI 생성' : 'AI 재생성' }}
                </button>
                <button
                  type="button"
                  :disabled="row.status !== 'DRAFT' || anyBusy"
                  @click="approve(row.segment)"
                >
                  {{ approvingSegment === row.segment ? '승인 중...' : '승인' }}
                </button>
                <button type="button" :disabled="anyBusy" @click="startEdit(row)">수정</button>
                <button
                  type="button"
                  :disabled="row.status === 'NOT_GENERATED' || anyBusy"
                  @click="removeVariant(row.segment)"
                >
                  {{ deletingSegment === row.segment ? '삭제 중...' : '삭제' }}
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.description-panel {
  padding: 16px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 6px;
}
.loading,
.error {
  font-size: 13px;
}
.error {
  color: #a80000;
}
.hint {
  color: #777;
  font-size: 12px;
  margin: 12px 0;
}
.generate-all-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}
.generate-all-button {
  padding: 8px 16px;
  border: 1px solid #0056b3;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.generate-all-button:disabled {
  background: #ccc;
  border-color: #ccc;
  cursor: default;
}
.base-description h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #444;
}
.image-extract-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.image-url-input {
  flex: 1;
  padding: 8px 10px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.image-extract-bar button {
  flex-shrink: 0;
  padding: 8px 14px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #fff;
  color: #0056b3;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
.image-extract-bar button:disabled {
  color: #999;
  border-color: #ccc;
  cursor: default;
}
.base-description textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
  resize: vertical;
}
.base-description-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}
.variant-groups {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.variant-group h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #444;
}
.variant-row {
  border-top: 1px solid #eee;
  padding: 10px 0;
}
.variant-row-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.segment-label {
  font-size: 12px;
  font-weight: 600;
  color: #333;
}
.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #eee;
  color: #666;
}
.status-badge.draft {
  background: #fff3cd;
  color: #8a6500;
}
.status-badge.approved {
  background: #d4edda;
  color: #256029;
}
.variant-content {
  font-size: 12px;
  color: #333;
  white-space: pre-wrap;
  margin: 4px 0 8px;
}
.edit-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 8px;
  font-size: 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  resize: vertical;
  margin: 4px 0 8px;
}
.variant-content.empty {
  color: #999;
}
.variant-row-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.variant-row-footer button {
  padding: 4px 10px;
  font-size: 12px;
  border: 1px solid #0056b3;
  border-radius: 4px;
  background: #0056b3;
  color: #fff;
  cursor: pointer;
}
.variant-row-footer button:disabled {
  background: #ccc;
  border-color: #ccc;
  cursor: default;
}
</style>
