package com.zcommcx.avatar.ai;

import com.zcommcx.avatar.domain.ShowhostDiscount;
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

    // 방송 특가는 대본(scriptPrompt)에는 넣지 않는다 — 대본은 상품당 1건 캐시라 설정이 바뀌어도 다시
    // 만들어지지 않고, AI가 할인율을 틀리게 말할 수도 있어서 특가 안내 멘트는 프론트가 고정 문구로 붙인다.
    // Q&A는 매번 새로 답하므로 현재 설정을 그대로 넘겨 "할인 없나요?"에 정확히 답하게 한다.
    static String qnaPrompt(Product product, String reviewSummary, ShowhostDiscount discount, String question) {
        return """
                당신은 라이브 커머스 방송 중인 AI 쇼호스트입니다. 시청자가 채팅으로 질문을 남기면
                아래 상품 정보/리뷰 요약만 근거로 답하세요. 근거가 없는 내용은 추측해서 답하지 말고
                "그 부분은 정확히 확인이 어렵다"는 취지로 솔직하게 답하세요.
                답변은 방송에서 소리 내어 읽을 것이므로 1~3문장의 짧고 자연스러운 구어체로 답하세요.

                %s
                [방송 특가]
                %s

                [시청자 질문]
                %s
                """.formatted(productContext(product, reviewSummary), discountContext(product, discount), question);
    }

    private static String discountContext(Product product, ShowhostDiscount discount) {
        if (!discount.isEnabled() || product.getPrice() == null) {
            return "현재 진행 중인 할인은 없습니다. 할인을 물으면 지금은 진행 중인 할인이 없다고 솔직하게 답하세요.";
        }
        long discounted = ShowhostDiscount.discountedPrice(product.getPrice(), discount.getDiscountRate());
        return """
                이 방송을 보고 있는 고객에게만 %d%% 할인(%d원 → %d원)을 드립니다.
                방송에 들어온 시점부터 %d분 안에, 화면의 "방송 특가로 구매" 버튼으로 주문해야 적용됩니다.
                한 고객당 상품별로 하루 1번, 주문 1건에만 쓸 수 있습니다."""
                .formatted(discount.getDiscountRate(), product.getPrice(), discounted, discount.getValidMinutes());
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
