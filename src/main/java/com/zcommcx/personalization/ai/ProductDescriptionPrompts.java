package com.zcommcx.personalization.ai;

import com.zcommcx.personalization.domain.CustomerSegment;

final class ProductDescriptionPrompts {

    private ProductDescriptionPrompts() {
    }

    static String generatePrompt(String productName, String brand, String baseDescription,
                                  CustomerSegment segment, String keywords) {
        String keywordLine = (keywords == null || keywords.isBlank())
                ? "특별히 지정된 키워드는 없습니다. 세그먼트 특성에 맞게 자연스럽게 작성하세요."
                : keywords;

        return """
                당신은 이커머스 상품 상세설명을 고객 성향(세그먼트)에 맞게 다시 쓰는 AI 카피라이터입니다.
                아래 기본 상품 상세설명에 담긴 사실만 활용해서, 지정된 세그먼트의 취향에 맞는 톤과
                강조 포인트로 상세설명을 새로 작성하세요.

                [작성 지침]
                - 기본 상세설명에 없는 사실(소재, 원산지, 기능 등)을 새로 지어내지 마세요. 과장·허위 광고 금지.
                - 운영자가 지정한 키워드를 자연스럽게 녹여 강조하세요.
                - 한국어로 3~5문장 분량, 마크다운/이모지 없이 순수 텍스트로 작성하세요.
                - 모든 문장을 한 줄로 이어쓰지 말고, 문장(또는 의미 단위)마다 줄바꿈으로 구분해서
                  가독성 있게 작성하세요.
                - 작성을 마친 뒤, 완성한 설명(description)이 [운영자 지정 키워드]를 얼마나 자연스럽고
                  충실하게 반영했는지 스스로 0~100점(fitScore)으로 채점하고, 그 이유를 1~2문장
                  한국어(fitScoreReason)로 남기세요. 지정된 키워드가 없다면 세그먼트 특성을 얼마나
                  잘 반영했는지를 기준으로 채점하세요. 점수는 후하게 주지 말고, 키워드가 실제로
                  드러나지 않은 부분이 있으면 감점하세요.

                [상품명] %s
                [브랜드] %s
                [기본 상세설명]
                %s

                [타겟 세그먼트] %s
                [운영자 지정 키워드] %s
                """.formatted(
                productName,
                brand == null || brand.isBlank() ? "정보 없음" : brand,
                baseDescription,
                segment.getLabel(),
                keywordLine);
    }

    static String extractFromImagePrompt() {
        return """
                당신은 이미지에 있는 상품 상세설명 텍스트를 그대로 추출하는 AI입니다.
                아래 이미지를 읽고, 상품 설명과 관련된 텍스트를 모두 추출해서 정리해 반환하세요.

                [작성 지침]
                - 이미지에 없는 내용을 새로 지어내지 마세요.
                - 광고 배너 문구, 브랜드 로고, 워터마크 등 상품 설명과 무관한 텍스트는 제외하세요.
                - 마크다운 없이 순수 텍스트로 작성하고, 문단 구분은 줄바꿈으로 표현하세요.
                - 상품 설명으로 볼 만한 텍스트가 이미지에 전혀 없으면 빈 문자열을 반환하세요.
                """;
    }

    static String cleanupScrapedTextPrompt(String rawText) {
        return """
                당신은 웹페이지에서 그대로 긁어온 텍스트 중 상품 상세설명에 해당하는 부분만 정리해서
                추출하는 AI입니다.

                [작성 지침]
                - 네비게이션 메뉴, 로그인/장바구니 안내, 배송·교환·환불 정책, 리뷰, 관련상품 추천,
                  광고 문구 등 상품 자체에 대한 설명이 아닌 텍스트는 모두 제외하세요.
                - 원문에 없는 내용을 새로 지어내지 마세요.
                - 마크다운 없이 순수 텍스트로 작성하고, 문단 구분은 줄바꿈으로 표현하세요.
                - 상품 설명으로 볼 만한 내용이 전혀 없으면 빈 문자열을 반환하세요.

                [원문]
                %s
                """.formatted(rawText);
    }
}
