package com.zcommcx.personalization.ai;

import com.zcommcx.personalization.domain.CustomerSegment;

import java.util.List;

final class SegmentKeywordPrompts {

    private SegmentKeywordPrompts() {
    }

    static String suggestPrompt(CustomerSegment segment, List<String> reviewExcerpts, String existingKeywords) {
        StringBuilder excerptBlock = new StringBuilder();
        for (int i = 0; i < reviewExcerpts.size(); i++) {
            excerptBlock.append(i + 1).append(". ").append(reviewExcerpts.get(i)).append('\n');
        }

        String existingLine = (existingKeywords == null || existingKeywords.isBlank())
                ? "(아직 등록된 키워드 없음)"
                : existingKeywords;

        return """
                당신은 이커머스 리뷰 데이터를 분석해서, 특정 고객 세그먼트의 상품 상세설명 작성에
                참고할 키워드를 제안하는 AI 분석가입니다.

                아래는 여러 상품에 대해 "%s" 세그먼트로 확인된 고객들이 실제로 작성한 리뷰 발췌입니다.

                [작성 지침]
                - 리뷰에서 반복적으로 나타나는 긍정적인 특징/니즈/구매 이유를 근거로 키워드를 제안하세요.
                - 리뷰에 실제로 나타나지 않은 내용을 추측해서 지어내지 마세요.
                - 키워드는 짧은 명사구(1~4단어)로, 5~8개 제안하세요.
                - 이미 등록된 키워드와 중복되는 표현은 되도록 피하고, 새로운 키워드 위주로 제안하세요.
                  다만 리뷰에서 압도적으로 반복되는 특징이라면 겹쳐도 됩니다.
                - 리뷰 수가 너무 적거나 내용이 빈약해 의미 있는 특징을 뽑기 어려우면 빈 배열을 반환하세요.

                [기존 등록 키워드] %s

                [리뷰 발췌]
                %s
                """.formatted(segment.getLabel(), existingLine, excerptBlock.toString().stripTrailing());
    }

    /**
     * 리뷰가 너무 적어(MIN_REVIEWS_FOR_SUGGESTION 미만) 리뷰 기반 분석이 불가능할 때 쓰는 프롬프트.
     * 실제 리뷰 데이터 없이 AI의 일반 지식만으로 키워드를 제안하고, 관리자가 직접 검색해볼 검색어도
     * 함께 요청한다 — 둘 다 "리뷰 기반"이 아니라 "참고용"임을 프론트에서 별도로 표시한다.
     */
    static String fallbackPrompt(CustomerSegment segment, String existingKeywords) {
        String existingLine = (existingKeywords == null || existingKeywords.isBlank())
                ? "(아직 등록된 키워드 없음)"
                : existingKeywords;

        return """
                당신은 이커머스 상품 상세설명 작성에 참고할 키워드를 제안하는 AI 분석가입니다.

                "%s" 세그먼트 고객이 작성한 리뷰가 아직 부족해서 리뷰 기반 분석은 할 수 없습니다.
                대신 일반적인 소비자 행동/구매 심리 지식을 바탕으로 아래 두 가지를 제안해주세요.

                [1. 키워드 제안]
                - 이 세그먼트가 상품을 구매할 때 중요하게 여길 만한 요인 위주로 5~8개 제안하세요
                  (짧은 명사구, 1~4단어). 예: 실용성, 가성비, 브랜드 신뢰도, 스펙/성능, 효율/시간 절약 같은
                  "구매 결정 요인"에 가까운 키워드를 우선하고, 근거 없는 유행 스타일명은 피하세요.
                - 이미 등록된 키워드와 중복되는 표현은 피하세요.
                - 일반 지식으로도 의미 있는 제안이 어려우면 빈 배열을 반환하세요.

                [2. 검색어 제안]
                - 관리자가 직접 인터넷에 검색해서 이 세그먼트의 최신 소비 트렌드/구매 요인을 찾아볼 수 있는
                  검색어를 1개 만들어주세요. 패션 유행어가 아니라 구매 결정 요인을 찾는 검색어여야 합니다.

                [기존 등록 키워드] %s
                """.formatted(segment.getLabel(), existingLine);
    }
}
