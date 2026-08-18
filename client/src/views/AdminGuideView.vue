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
  inProgress?: boolean
}

// 이 플랫폼은 원래 별개였던 조별과제들을 하나씩 통합해서 만들어졌다. 새 과제가 들어올 때마다
// 이 배열에 섹션 하나만 더 추가하면 되도록 구성했다.
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
    badge: '③ ai-product-personalization',
    title: 'AI 기반 고객 맞춤형 상품 상세설명',
    summary:
      '고객의 성별·연령 성향(6개 세그먼트)에 맞춰 AI가 상품 상세설명을 다르게 만들어 보여줍니다. 세그먼트별 키워드는 관리자가 입력(리뷰 기반 AI 추천도 참고 가능)하고, AI는 설명을 생성하면서 스스로 적합도까지 평가합니다.',
    customerFlow: [
      { label: '로그인', to: '/login', desc: '이름/전화번호를 입력합니다. 처음 로그인하는 경우에만 성별·출생년도(둘 다 선택)를 추가로 입력받습니다. 연령 대신 출생년도를 받는 이유는 시간이 지나도 다시 입력할 필요 없이 나이가 항상 정확히 계산되기 때문입니다.' },
      {
        label: '상품상세',
        desc: '리뷰 위쪽에서 상세설명을 확인합니다. 내 성별·연령에 맞는 승인된 설명이 있으면 그걸, 없으면 기본 설명을 보여주며, 맞춤 설명일 땐 "고객님을 위한 맞춤 설명" 배지가 붙습니다.',
      },
      { label: '내 정보', desc: '헤더의 이름을 클릭하면 성별·출생년도를 언제든 수정할 수 있고, 저장하면 보고 있던 상품상세 설명도 즉시 다시 조회됩니다.' },
    ],
    adminFlow: [
      {
        label: '성향키워드',
        to: '/admin/segment-keywords',
        desc: '상품관리 화면의 "성향키워드 관리" 버튼으로 들어와, 성별×연령 6개 세그먼트별로 강조할 키워드를 전체 상품 공통으로 입력합니다. "AI 추천 키워드" 버튼을 누르면 그 세그먼트 고객이 실제로 쓴 리뷰(리뷰 작성 시점 성별·연령 기준)를 AI가 분석해 키워드 후보를 제안합니다 — 후보를 클릭하면 입력창에 추가되고, 저장은 별도로 눌러야 합니다. 분석 가능한 리뷰가 3건 미만이면 제안하지 않습니다.',
      },
      {
        label: '상품 상세설명 관리',
        desc: '상품관리 목록의 "상세설명 관리" 버튼으로 상품별 화면에 들어가, 기본 설명을 입력(직접 입력 또는 URL·이미지에서 텍스트 추출)하고 세그먼트별로 AI 생성(개별/전체)·직접 수정·삭제·승인을 진행합니다. 승인해야 고객화면에 노출됩니다. AI가 생성한 설명에는 스스로 매긴 "적합도 점수"(운영자 키워드를 얼마나 반영했는지, 0~100점)와 그 평가 근거가 함께 표시되며, 내용을 직접 수정하면 이 점수는 더 이상 유효하지 않으므로 사라집니다.',
      },
    ],
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

    <section class="login-note">
      <p><strong>사이트 로그인</strong>(Google 계정) — 이 배포에 들어올 자격이 있는지 확인하는 관문입니다. 운영자어드민·고객화면 전체를 이용하려면 먼저 필요합니다.</p>
      <p><strong>회원 로그인</strong>(이름/전화번호) — 사이트 로그인 이후, 고객화면에서 리뷰 작성·주문·CS채팅 등 고객 기능을 쓸 때만 필요한 별도의 간단한 신원입니다.</p>
    </section>

    <section v-for="section in sections" :key="section.badge" class="guide-section" :class="{ placeholder: section.placeholder }">
      <div class="section-header">
        <span class="badge">{{ section.badge }}</span>
        <h3>{{ section.title }}</h3>
        <span v-if="section.inProgress" class="status-tag">진행중</span>
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
.login-note {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  background: #f5f7fa;
  border: 1px solid #e3e6ea;
  border-radius: 8px;
}
.login-note p {
  margin: 0;
  font-size: 13px;
  color: #555;
  line-height: 1.5;
}
.login-note strong {
  color: #222;
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
.status-tag {
  padding: 2px 10px;
  border-radius: 999px;
  background: #fde2c8;
  color: #a35b00;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
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
