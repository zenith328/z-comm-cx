<script setup lang="ts">
import { onMounted, ref } from 'vue'
import '../styles/admin.css'
import { fetchAvatarSetting, updateAvatarSetting } from '../api/avatarSetting'

const imageUrl = ref('')
const savedImageUrl = ref<string | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const savedMessage = ref('')

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const setting = await fetchAvatarSetting()
    savedImageUrl.value = setting.imageUrl
    imageUrl.value = setting.imageUrl ?? ''
  } catch (error) {
    console.error(error)
    errorMessage.value = '아바타 설정을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!imageUrl.value.trim()) return
  saving.value = true
  errorMessage.value = ''
  savedMessage.value = ''
  try {
    const setting = await updateAvatarSetting(imageUrl.value.trim())
    savedImageUrl.value = setting.imageUrl
    savedMessage.value = '저장했습니다.'
  } catch (error) {
    console.error(error)
    errorMessage.value = '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <h2 class="admin-title">AI 쇼호스트 아바타 설정</h2>
    <p class="admin-muted">
      상품별이 아니라 사이트 전체에서 공통으로 쓰는 정지 이미지 1장입니다. 움직이는 애니메이션이나
      상품별 아바타는 이번 범위에 포함하지 않았습니다.
    </p>

    <p v-if="loading" class="admin-muted">불러오는 중...</p>

    <template v-else>
      <div class="preview-box">
        <img v-if="savedImageUrl" :src="savedImageUrl" alt="현재 아바타 이미지" class="preview-image" />
        <span v-else class="preview-empty">등록된 이미지가 없습니다</span>
      </div>

      <form class="setting-form" @submit.prevent="save">
        <label>
          아바타 이미지 URL
          <input v-model="imageUrl" type="text" placeholder="https://..." :disabled="saving" />
        </label>
        <button type="submit" :disabled="saving || !imageUrl.trim()">
          {{ saving ? '저장 중...' : '저장' }}
        </button>
      </form>

      <p v-if="errorMessage" class="admin-error">{{ errorMessage }}</p>
      <p v-else-if="savedMessage" class="admin-muted">{{ savedMessage }}</p>
    </template>
  </div>
</template>

<style scoped>
.preview-box {
  width: 200px;
  height: 260px;
  margin: 12px 0 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-empty {
  font-size: 13px;
  color: #999;
  text-align: center;
  padding: 0 12px;
}
.setting-form {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 8px;
}
.setting-form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #555;
}
.setting-form input {
  width: 360px;
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 14px;
}
.setting-form button {
  padding: 9px 16px;
  border: none;
  border-radius: 6px;
  background: #0056b3;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
.setting-form button:disabled {
  opacity: 0.6;
  cursor: default;
}
</style>
