import { reactive } from 'vue'

// 상품상세/리뷰작성처럼 라우트 파라미터만으로는 알 수 없는 브레드크럼 라벨(상품명)을
// 해당 화면이 데이터를 불러온 뒤 채워 넣기 위한 공유 상태.
export const breadcrumbState = reactive<{ productName: string | null }>({
  productName: null,
})

export function setBreadcrumbProductName(name: string | null) {
  breadcrumbState.productName = name
}
