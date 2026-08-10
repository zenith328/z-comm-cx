package com.zcommcx.review.ai;

import com.zcommcx.review.domain.Review;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class ReviewPrompts {

    private ReviewPrompts() {
    }

    static String classificationPrompt(Review review) {
        return """
                당신은 이커머스 상품평 운영을 돕는 AI 심사자입니다.
                아래 리뷰를 분석해서 visible, classification, sentiment, risk_score, reason을
                JSON으로 반환하세요. visible, classification, sentiment는 서로 독립적인 값입니다.

                [visible 기준 - 구매자에게 공개해도 되는 리뷰인지]
                - false: 욕설/비방, 의미 없는 도배글(예: ㅋㅋㅋㅋ), 타사 언급, 광고성 리뷰
                - true: 그 외 정상적인 리뷰
                - visible이 false이면 classification은 항상 NONE으로 하세요.

                [classification 기준 - 공개 리뷰 중에서의 품질 등급]
                - BEST_CANDIDATE: 사진이 있고 사이즈/재질/장단점 등 구체적 정보가 진솔하게 담긴 우수 리뷰
                - RECOMMENDED: 사이즈, 배송, 착용감 등 구매에 실질적으로 도움이 되는 정보성 리뷰
                - NONE: 위 두 가지에 해당하지 않는 평범한 리뷰

                [sentiment 기준 - 리뷰의 전반적인 논조]
                - POSITIVE: 만족/추천 등 긍정적인 의견이 주를 이룸
                - NEGATIVE: 불만/단점/문제 제기 등 부정적인 의견이 주를 이룸
                - NEUTRAL: 감정 표현 없이 사실만 전달하거나, 장단점이 비슷한 비중으로 섞여 있음

                risk_score는 0~100 사이 정수로, 비공개 처리가 필요한 위험도를 의미합니다.

                [예시]
                입력: "배송 늦고 사이즈 완전 안맞음. 재질도 별로. 사진 첨부함" (사진 있음, 별점 3)
                출력: {"visible": true, "classification": "BEST_CANDIDATE", "sentiment": "NEGATIVE", "risk_score": 5, "reason": "사이즈, 재질, 배송에 대한 구체적 정보를 담은 진솔한 포토 리뷰."}

                입력: "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ" (사진 없음, 별점 5)
                출력: {"visible": false, "classification": "NONE", "sentiment": "NEUTRAL", "risk_score": 90, "reason": "내용 없이 의미 없는 문자만 반복된 도배성 리뷰."}

                [분석 대상 리뷰]
                별점: %d
                사진 첨부 여부: %s
                내용: %s
                """.formatted(review.getRating(), review.isHasPhoto() ? "있음" : "없음", review.getContent());
    }

    static String summaryPrompt(List<Review> reviews, String query) {
        String reviewLines = IntStream.range(0, reviews.size())
                .mapToObj(i -> {
                    Review review = reviews.get(i);
                    return "%d. (별점 %d, 사진 %s) %s".formatted(
                            i + 1, review.getRating(), review.isHasPhoto() ? "있음" : "없음", review.getContent());
                })
                .collect(Collectors.joining("\n"));

        return """
                당신은 이커머스 구매자에게 리뷰를 요약해주는 AI 어시스턴트입니다.
                아래는 한 상품의 실제 구매자 리뷰 목록입니다. 구매자의 질문에 맞는 내용만 리뷰에서 뽑아
                간결하게 요약하세요. 리뷰에 없는 내용은 추측해서 만들어내지 마세요. 관련 내용이 리뷰에
                없으면 "관련 언급을 찾을 수 없습니다"라고 답하세요.

                [구매자 질문]
                %s

                [리뷰 목록]
                %s
                """.formatted(query, reviewLines);
    }
}
