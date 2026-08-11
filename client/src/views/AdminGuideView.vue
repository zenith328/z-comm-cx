<script setup lang="ts">
interface FlowStep {
  label: string
  to?: string
  desc: string
}

interface GuideSection {
  badge: string
  title: string
  summary: string
  customerFlow: FlowStep[]
  adminFlow: FlowStep[]
  placeholder?: boolean
}

// 이 플랫폼은 원래 별개였던 조별과제들을 하나씩 통합해서 만들어졌다. 세 번째 과제도 곧 추가될
// 예정이라, 새 과제가 들어올 때마다 이 배열에 섹션 하나만 더 추가하면 되도록 구성했다.
const sections: GuideSection[] = [
  {
    badge: '① ai-review-management',
    title: 'AI 리뷰 매니지먼트',
    summary:
      '상품을 등록하면 고객이 리뷰를 남기고, AI가 리뷰마다 공개여부·분류·감성을 자동으로 판단합니다.',
    customerFlow: [
      { label: '상품목록', to: '/products', desc: '브랜드 필터·정렬로 상품을 둘러봅니다.' },
      { label: '상품상세', desc: 'AI 리뷰 요약봇에게 질문하고, 이번 주 베스트 리뷰·리뷰 목록을 확인합니다.' },
      { label: '리뷰 작성', desc: '상품상세 화면의 "리뷰 작성하기" 버튼으로 별점/사진첨부 여부와 함께 리뷰를 남깁니다.' },
    ],
    adminFlow: [
      { label: '상품관리', to: '/admin/products', desc: '쇼핑몰 URL로 상품을 등록(자동 스크래핑)하고, 재고를 입고/품절 처리합니다.' },
      {
        label: '리뷰관리',
        to: '/admin/reviews',
        desc: 'AI가 내린 공개여부·분류 판단을 확인하고 필요하면 수동으로 override합니다. 분석 실패(FAILED) 리뷰는 재시도 버튼으로 재분석하고, 베스트 리뷰 숏리스트를 수동으로 재생성할 수 있습니다.',
      },
    ],
  },
  {
    badge: '② ai-cs-auto-resolver',
    title: 'AI CS 자동 해결 에이전트',
    summary:
      '고객의 자연어 CS 요청을 AI가 이해해서 주문 조회·취소·배송지 변경·반품접수 같은 실제 처리를 자동으로 수행합니다.',
    customerFlow: [
      { label: '주문하기', desc: '상품상세에서 수량을 정해 주문서를 작성합니다.' },
      { label: '주문목록', to: '/orders', desc: '주문 상태와 배송지 정보를 확인합니다.' },
      { label: 'CS채팅', to: '/chat', desc: '"주문번호 OOO 취소하고 싶어요"처럼 편하게 말하면 AI가 처리합니다.' },
    ],
    adminFlow: [
      { label: '주문관리', to: '/admin/orders', desc: '전체 주문을 조회하고 배송 상태를 처리합니다.' },
      {
        label: 'CS목록',
        to: '/admin/tickets',
        desc: 'AI가 하드룰(가드레일)에 걸려 자동이관한 건이나, AI 처리 실패 건을 상담원이 직접 응대합니다. 상단 "AI 자동이관 기준" 버튼에서 어떤 경우에 자동이관되는지 확인할 수 있습니다.',
      },
    ],
  },
  {
    badge: '③ 준비 중',
    title: '다음 과제',
    summary: '세 번째 과제가 곧 이 플랫폼 위에 통합될 예정입니다. 준비되는 대로 이 자리에 안내가 추가됩니다.',
    customerFlow: [],
    adminFlow: [],
    placeholder: true,
  },
]
</script>

<template>
  <div class="guide-page">
    <section class="intro">
      <h2>Z Commerce CX 사용법</h2>
      <p>
        이 플랫폼은 서로 다른 조별과제들을 하나의 커머스 고객경험(CX) 플랫폼으로 통합해서 만들어지고
        있습니다. 아래에서 과제별로 고객화면·운영자화면 사용법을 확인하세요.
      </p>
    </section>

    <section v-for="section in sections" :key="section.badge" class="guide-section" :class="{ placeholder: section.placeholder }">
      <div class="section-header">
        <span class="badge">{{ section.badge }}</span>
        <h3>{{ section.title }}</h3>
      </div>
      <p class="summary">{{ section.summary }}</p>

      <template v-if="!section.placeholder">
        <div class="flow-columns">
          <div class="flow-column">
            <h4>고객화면</h4>
            <ul>
              <li v-for="step in section.customerFlow" :key="step.label">
                <RouterLink v-if="step.to" :to="step.to" class="step-label">{{ step.label }}</RouterLink>
                <span v-else class="step-label">{{ step.label }}</span>
                <span class="step-desc">{{ step.desc }}</span>
              </li>
            </ul>
          </div>
          <div class="flow-column">
            <h4>운영자화면</h4>
            <ul>
              <li v-for="step in section.adminFlow" :key="step.label">
                <RouterLink v-if="step.to" :to="step.to" class="step-label">{{ step.label }}</RouterLink>
                <span v-else class="step-label">{{ step.label }}</span>
                <span class="step-desc">{{ step.desc }}</span>
              </li>
            </ul>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.guide-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.intro {
  padding-bottom: 4px;
}
.intro h2 {
  margin: 0 0 8px;
  font-size: 20px;
}
.intro p {
  margin: 0;
  color: #555;
  font-size: 14px;
  line-height: 1.6;
}
.guide-section {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 20px 24px;
  background: #fafbfc;
}
.guide-section.placeholder {
  background: #f5f5f5;
  border-style: dashed;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.badge {
  padding: 2px 10px;
  border-radius: 999px;
  background: #dceeff;
  color: #0056b3;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.placeholder .badge {
  background: #e6e6e6;
  color: #888;
}
.section-header h3 {
  margin: 0;
  font-size: 16px;
}
.summary {
  margin: 0 0 16px;
  color: #555;
  font-size: 13px;
  line-height: 1.6;
}
.placeholder .summary {
  margin-bottom: 0;
}
.flow-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.flow-column h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #888;
}
.flow-column ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.flow-column li {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.step-label {
  font-size: 13px;
  font-weight: 600;
  color: #222;
  text-decoration: none;
  width: fit-content;
}
a.step-label {
  color: #0056b3;
}
a.step-label:hover {
  text-decoration: underline;
}
.step-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

@media (max-width: 700px) {
  .flow-columns {
    grid-template-columns: 1fr;
  }
}
</style>
