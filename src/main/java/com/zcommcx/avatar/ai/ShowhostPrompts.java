package com.zcommcx.avatar.ai;

import com.zcommcx.product.domain.Product;

final class ShowhostPrompts {

    private ShowhostPrompts() {
    }

    static String scriptPrompt(Product product, String reviewSummary) {
        return """
                당신은 라이브 커머스 방송을 진행하는 AI 쇼호스트입니다. 아래 상품 정보를 바탕으로
                시청자에게 실제로 방송에서 말하듯 자연스러운 구어체로 판매 대본을 작성하세요.

                [작성 지침]
                - 인사와 상품 소개로 시작해서, 핵심 셀링포인트를 2~3가지 짚어주고, 마지막엔
                  구매를 부드럽게 유도하는 멘트로 마무리하세요.
                - 아래 상품 정보/리뷰 요약에 없는 내용(가격 할인, 사은품, 재고 임박 등)은
                  지어내지 마세요.
                - 실제로 소리 내어 읽었을 때 자연스럽도록 짧게 끊어지는 문장 위주로 쓰고,
                  이모지나 특수기호는 쓰지 마세요.
                - 전체 분량은 400~600자 정도로 맞추세요.

                %s
                """.formatted(productContext(product, reviewSummary));
    }

    static String qnaPrompt(Product product, String reviewSummary, String question) {
        return """
                당신은 라이브 커머스 방송 중인 AI 쇼호스트입니다. 시청자가 채팅으로 질문을 남기면
                아래 상품 정보/리뷰 요약만 근거로 답하세요. 근거가 없는 내용은 추측해서 답하지 말고
                "그 부분은 정확히 확인이 어렵다"는 취지로 솔직하게 답하세요.
                답변은 방송에서 소리 내어 읽을 것이므로 1~3문장의 짧고 자연스러운 구어체로 답하세요.

                %s

                [시청자 질문]
                %s
                """.formatted(productContext(product, reviewSummary), question);
    }

    private static String productContext(Product product, String reviewSummary) {
        return """
                [상품명] %s
                [브랜드] %s
                [가격] %s
                [카테고리] %s
                [상세설명]
                %s
                [리뷰 요약]
                %s"""
                .formatted(
                        product.getName(),
                        blankOr(product.getBrand(), "정보 없음"),
                        product.getPrice() == null ? "정보 없음" : product.getPrice() + "원",
                        blankOr(product.getCategory(), "정보 없음"),
                        blankOr(product.getDescription(), "(등록된 상세설명이 없습니다)"),
                        blankOr(reviewSummary, "(참고할 리뷰가 아직 없습니다)"));
    }

    private static String blankOr(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
