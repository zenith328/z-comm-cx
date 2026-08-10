<script setup lang="ts">
interface ExampleGroup {
  key: string
  label: string
  description: string
  examples: string[]
}

defineProps<{ open: boolean }>()
const emit = defineEmits<{ select: [content: string]; close: [] }>()

const EXAMPLE_GROUPS: ExampleGroup[] = [
  {
    key: 'HIDDEN',
    label: '비공개',
    description: 'AI가 욕설·도배·광고성 등으로 판단해 비공개 처리할 가능성이 높은 예시입니다 (참고용).',
    examples: [
      '구려요 최악 환불해주세요!!!',
      '여기서 사지 마세요 짝퉁 팝니다 저희 사이트 가입하면 할인해드려요 www.example.com',
      'ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ',
      '판매자 진짜 사기꾼임 신고합니다 전화도 안받고 최악 다시는 안삼',
      '테스트입니다 무시하세요',
    ],
  },
  {
    key: 'RECOMMENDED',
    label: '추천',
    description: '구체적인 만족 포인트가 담긴 무난한 추천 리뷰 예시입니다.',
    examples: [
      '사이즈 잘 맞고 재질도 부드러워서 만족스럽습니다. 색상도 사진이랑 비슷해요.',
      '가볍고 튼튼해서 잘 쓰고 있어요. 배송도 빨랐습니다.',
      '디자인이 심플하고 활동성이 좋아요. 다음에 다른 색상도 구매하고 싶어요.',
      '생각보다 퀄리티가 좋네요. 가격 대비 만족스러운 제품입니다.',
      '착용감이 편안하고 세탁 후에도 변형이 없어서 좋아요.',
    ],
  },
  {
    key: 'BEST',
    label: '베스트',
    description: '사용 기간·장단점·사이즈 팁 등을 구체적으로 담은 상세 리뷰 예시입니다.',
    examples: [
      '3주 정도 매일 착용해본 후기 남겨요. 사이즈는 평소보다 반 사이즈 크게 나와서 한 치수 작게 주문하시는 걸 추천해요. 통기성이 좋아서 여름철에도 쾌적했고, 오래 걸어도 발이 편했습니다. 색상은 사진보다 실물이 조금 더 밝은 편이고, 세탁 후에도 변형 없이 잘 유지됩니다. 가격대를 고려하면 정말 만족스러운 구매였어요.',
      '장시간 사용에도 무게 부담이 없었습니다. 방수 기능도 실제 소나기에서 테스트해봤는데 안까지 스며들지 않았고, 지지력도 좋아서 험한 길에서도 안정적이었어요. 사이즈는 정사이즈로 편하게 잘 맞았고, 디자인도 무난해서 캐주얼하게도 활용하기 좋습니다.',
      '사진 첨부합니다. 실착해보면 핏이 예쁘게 떨어지고, 원단이 두껍지 않아 사계절 다 활용 가능할 것 같아요. 늘어남도 없고 봉제 마감도 꼼꼼해서 오래 입을 수 있을 것 같습니다. 사이즈 표기도 상세페이지와 거의 일치했어요.',
      '장단점을 솔직히 적어볼게요. 장점은 가볍고 수납공간이 넉넉하고 쿠션감이 좋아 장시간 사용해도 편함. 단점은 지퍼가 조금 뻑뻑하고 색상이 사진보다 살짝 어두움. 그래도 전체적으로 완성도가 높고 가성비 좋은 제품이라 재구매 의사 있습니다.',
      '한 달 사용 후기입니다. 처음엔 조금 뻣뻣했는데 며칠 쓰니 몸에 맞게 길들여져서 지금은 제일 편하게 쓰고 있어요. 그립력도 좋아서 비 오는 날에도 안 미끄러웠고, 마모도 아직까지는 거의 없습니다. 사이즈 고민하시는 분들은 정사이즈로 가시면 될 것 같아요.',
    ],
  },
]

function choose(example: string) {
  emit('select', example)
}
</script>

<template>
  <div v-if="open" class="overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>예시 리뷰 선택</h3>
        <button type="button" class="close-button" @click="emit('close')">×</button>
      </div>
      <div class="modal-body">
        <section v-for="group in EXAMPLE_GROUPS" :key="group.key" class="example-group">
          <h4>{{ group.label }}</h4>
          <p class="group-description">{{ group.description }}</p>
          <ul class="example-list">
            <li v-for="(example, index) in group.examples" :key="index">
              <button type="button" class="example-item" @click="choose(example)">
                {{ example }}
              </button>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  width: 560px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 64px);
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.modal-header h3 {
  margin: 0;
  font-size: 16px;
}
.close-button {
  border: none;
  background: none;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #666;
  padding: 0;
}
.modal-body {
  padding: 16px 20px 20px;
  overflow-y: auto;
}
.example-group {
  margin-bottom: 20px;
}
.example-group:last-child {
  margin-bottom: 0;
}
.example-group h4 {
  margin: 0 0 4px;
  font-size: 14px;
}
.group-description {
  margin: 0 0 10px;
  font-size: 12px;
  color: #888;
}
.example-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.example-item {
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.4;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #fafafa;
  cursor: pointer;
}
.example-item:hover {
  background: #f0f6ff;
  border-color: #99c2ff;
}
</style>
