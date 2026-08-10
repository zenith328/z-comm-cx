<script setup lang="ts">
defineProps<{ hasPhoto: boolean }>()
</script>

<template>
  <span class="badge" :class="{ image: hasPhoto, text: !hasPhoto }">{{
    hasPhoto ? '이미지리뷰' : '텍스트리뷰'
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
.text {
  background: #eeeeee;
  color: #555555;
}
.image {
  background: #fff0d6;
  color: #a15c00;
}
</style>
