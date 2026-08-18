package com.zcommcx.review.ai;

import com.zcommcx.review.domain.Review;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class FitGuidePrompts {

    private FitGuidePrompts() {
    }

    /**
     * shoulderFit/chestFit/lengthFit이라는 JSON 키 이름은 고정이지만, 실제로 무엇에 대한 핏인지는
     * 카테고리별 축 라벨(labels)에 맞춰 매번 다시 설명해준다 — 그래야 신발인데 "어깨" 핏을
     * 묻는 것처럼 어색해지지 않는다.
     */
    private static String outputSpec(FitAxisLabels.Labels labels) {
        return """
                [출력 값 설명]
                - shoulderFit: "%s" 핏을 TIGHT(타이트함) / TRUE_TO_SIZE(정사이즈) / LOOSE(넉넉함) /
                  UNKNOWN(정보 부족) 중 하나로 답하세요.
                - chestFit: "%s" 핏을 같은 기준으로 답하세요.
                - lengthFit: "%s" 핏을 같은 기준으로 답하세요.
                - recommendedBodyType: 이 상품이 잘 맞는다고 볼 수 있는 체형/사이즈 특징을 1문장으로
                  (근거가 없으면 "정보가 부족합니다"라고 답하세요).
                - summary: 핏에 대한 전반적인 총평 1~2문장.
                - 근거 없는 항목은 절대 추측해서 지어내지 말고 UNKNOWN/정보 부족으로 표기하세요.
                """.formatted(labels.axis1(), labels.axis2(), labels.axis3());
    }

    static String fromReviewsPrompt(List<Review> reviews, FitAxisLabels.Labels labels) {
        String reviewLines = IntStream.range(0, reviews.size())
                .mapToObj(i -> {
                    Review review = reviews.get(i);
                    return "%d. (별점 %d) %s".formatted(i + 1, review.getRating(), review.getContent());
                })
                .collect(Collectors.joining("\n"));

        return """
                당신은 이커머스 리뷰에서 사이즈/핏 정보를 추출하는 AI 분석가입니다.
                아래는 한 상품에 대한 실제 구매자 리뷰 목록입니다. 리뷰에 언급된 사이즈/핏 관련
                내용(키, 몸무게, "타이트하다", "넉넉하다", "정사이즈" 등)만 근거로 판단하세요.

                %s

                [리뷰 목록]
                %s
                """.formatted(outputSpec(labels), reviewLines);
    }

    static String fromDescriptionPrompt(String productName, String category, String description, FitAxisLabels.Labels labels) {
        String descriptionLine = (description == null || description.isBlank())
                ? "(등록된 상세설명이 없습니다)"
                : description;
        String categoryLine = categoryLineOrGuessNotice(category);

        return """
                당신은 상품 상세설명만 보고 예상 핏을 조심스럽게 추정하는 AI입니다. 이 상품은 아직
                리뷰가 충분하지 않아 실제 착용 후기를 참고할 수 없습니다. 아래 상품명/카테고리/
                상세설명에 소재/핏/사이즈에 대한 직접적인 언급이 있으면 그것만 근거로 판단하고,
                없으면 모든 항목을 UNKNOWN/정보 부족으로 답하세요. 상세설명에 없는 내용을 추측해서
                지어내지 마세요.

                %s

                [상품명] %s
                [카테고리] %s
                [상세설명]
                %s
                """.formatted(outputSpec(labels), productName, categoryLine, descriptionLine);
    }

    static String syntheticReviewsPrompt(String productName, String brand, String category, String description, int count) {
        String descriptionLine = (description == null || description.isBlank())
                ? "(등록된 상세설명이 없습니다)"
                : description;
        String categoryLine = categoryLineOrGuessNotice(category);

        return """
                당신은 이커머스 상품에 대한 가상의 구매 후기를 만드는 AI입니다. 아래 상품에 대해
                서로 다른 체형/사이즈를 가진 가상 고객 %d명이 작성한 것처럼 리뷰를 %d개 만드세요.
                이 리뷰는 실제 고객 데이터가 아니라 AI 핏 가이드 기능의 프롬프트 테스트/시연
                전용으로만 쓰입니다.

                [작성 지침]
                - [카테고리]가 주어졌으면 그것을 최우선 근거로, 없으면 상세설명을 보고 이 상품이
                  실제로 무엇인지(신발/상의/하의/아우터 등) 먼저 파악한 다음, 그 카테고리에 맞는
                  표현만 쓰세요. 예를 들어 신발이면 발볼/사이즈/쿠셔닝/발등 같은 표현을 쓰고,
                  절대 어깨/가슴/소매처럼 의류에만 해당하는 표현을 쓰지 마세요. 반대로 상의/
                  아우터라면 어깨/가슴/기장/소매 표현을 쓰고 발볼 같은 신발 표현은 쓰지 마세요.
                - 각 리뷰에는 키/몸무게 같은 구체적인 체형 정보와, 실제 착용했을 때의 사이즈/핏
                  느낌("타이트하다", "넉넉하다", "정사이즈다" 등, 카테고리에 맞게)을 자연스러운
                  구어체 문장으로 포함하세요.
                - 체형 분포는 다양하게(마른 체형/보통 체형/통통한 체형, 작은 키/보통 키/큰 키 등
                  골고루) 만드세요.
                - 리뷰 하나당 2~4문장 정도로, 실제 이커머스 리뷰처럼 자연스럽게 작성하세요.
                - 별점(rating)은 1~5 사이 정수로, 대체로 만족(4~5점)이되 가끔 불만(2~3점)도 섞으세요.

                [상품명] %s
                [브랜드] %s
                [카테고리] %s
                [상세설명]
                %s
                """.formatted(
                count, count, productName, brand == null || brand.isBlank() ? "정보 없음" : brand, categoryLine,
                descriptionLine);
    }

    private static String categoryLineOrGuessNotice(String category) {
        return (category == null || category.isBlank())
                ? "(등록된 카테고리가 없습니다 — 상품명/상세설명을 보고 신중하게 판단하세요)"
                : category;
    }
}
