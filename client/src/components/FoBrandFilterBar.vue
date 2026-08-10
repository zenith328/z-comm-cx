<script setup lang="ts">
import { computed, ref } from 'vue'

const MAX_VISIBLE = 5

const props = defineProps<{ brands: string[]; selected: string | null }>()
const emit = defineEmits<{ select: [brand: string | null] }>()

const dropdownOpen = ref(false)

const visibleBrands = computed(() => props.brands.slice(0, MAX_VISIBLE))
const overflowBrands = computed(() => props.brands.slice(MAX_VISIBLE))
const isOverflowActive = computed(
  () => props.selected !== null && overflowBrands.value.includes(props.selected),
)

function select(brand: string | null) {
  dropdownOpen.value = false
  emit('select', brand)
}

function toggleDropdown() {
  if (overflowBrands.value.length === 0) return
  dropdownOpen.value = !dropdownOpen.value
}

function onOverflowChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  select(value || null)
}
</script>

<template>
  <div v-if="brands.length > 0" class="brand-filter-bar">
    <button type="button" class="brand-chip" :class="{ active: selected === null }" @click="select(null)">
      전체
    </button>
    <button
      v-for="brand in visibleBrands"
      :key="brand"
      type="button"
      class="brand-chip"
      :class="{ active: selected === brand }"
      @click="select(brand)"
    >
      {{ brand }}
    </button>
    <div v-if="overflowBrands.length > 0" class="brand-overflow">
      <button
        type="button"
        class="brand-chip"
        :class="{ active: isOverflowActive }"
        @click="toggleDropdown"
      >
        {{ isOverflowActive ? selected : '...' }}
      </button>
      <select
        v-if="dropdownOpen"
        class="brand-dropdown"
        :value="isOverflowActive ? selected ?? '' : ''"
        @change="onOverflowChange"
      >
        <option value="" disabled>브랜드 선택</option>
        <option v-for="brand in overflowBrands" :key="brand" :value="brand">{{ brand }}</option>
      </select>
    </div>
  </div>
</template>

<style scoped>
.brand-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.brand-chip {
  padding: 4px 12px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 16px;
  background: #fff;
  color: #333;
  cursor: pointer;
}
.brand-chip.active {
  background: #0056b3;
  border-color: #0056b3;
  color: #fff;
}
.brand-overflow {
  display: inline-flex;
  align-items: center;
}
.brand-dropdown {
  margin-left: 6px;
  font-size: 13px;
  padding: 3px 6px;
}
</style>
