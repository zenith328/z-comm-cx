package com.zcommcx.review.ai;

import com.zcommcx.member.domain.Gender;
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

    /**
     * 리뷰 요약 프롬프트. 리뷰마다 작성자 성별/연령대를 함께 넘겨, "여성 66 사이즈에 잘 맞음" 같은
     * 특정 체형 기준의 핏 후기를 상품 자체의 장점처럼 일반화하지 않게 한다. viewerGender가 있으면
     * 그 고객에게 해당하는 내용 위주로 요약하게 한다(없으면 특정 고객을 가정하지 않은 일반 요약).
     */
    static String summaryPrompt(List<Review> reviews, String query, Gender viewerGender) {
        String reviewLines = IntStream.range(0, reviews.size())
                .mapToObj(i -> {
                    Review review = reviews.get(i);
                    return "%d. (작성자 %s, 별점 %d, 사진 %s) %s".formatted(
                            i + 1, reviewerLabel(review), review.getRating(),
                            review.isHasPhoto() ? "있음" : "없음", review.getContent());
                })
                .collect(Collectors.joining("\n"));

        return """
                당신은 이커머스 구매자에게 리뷰를 요약해주는 AI 어시스턴트입니다.
                아래는 한 상품의 실제 구매자 리뷰 목록입니다. 구매자의 질문에 맞는 내용만 리뷰에서 뽑아
                간결하게 요약하세요. 리뷰에 없는 내용은 추측해서 만들어내지 마세요. 관련 내용이 리뷰에
                없으면 "관련 언급을 찾을 수 없습니다"라고 답하세요.

                [요약 원칙]
                - 장점/단점은 소재, 무게, 기능, 마감, 세탁·관리, 배송처럼 누가 사도 똑같이 적용되는
                  상품 자체의 특성을 중심으로 정리하세요.
                - "66 사이즈에 잘 맞음", "엉덩이를 가려줌"처럼 작성자 개인의 체형·성별에 따라 달라지는
                  사이즈/핏 후기는 상품의 일반적인 장점처럼 쓰지 마세요. 질문이 사이즈/핏에 관한 것이라
                  꼭 필요할 때만 "여성 구매자 기준"처럼 누구 기준인지 밝혀서 쓰세요.
                - 작성자 정보가 "정보 없음"이어도 본문에 성별·체형이 드러나면(예: "여자용 66 입는데",
                  "남편이 입어보니") 그 내용을 기준으로 판단하세요. 본문에도 없으면 추측하지 마세요.
                %s
                [구매자 질문]
                %s

                [리뷰 목록]
                %s
                """.formatted(viewerGuide(viewerGender), query, reviewLines);
    }

    private static String viewerGuide(Gender viewerGender) {
        if (viewerGender == null) {
            return "";
        }
        String viewer = genderLabel(viewerGender);
        return """

                [이 요약을 보는 고객]
                - %s 고객입니다. 이 고객이 실제로 구매했을 때 해당되는 내용 위주로 요약하세요.
                - 사이즈/핏/착용감은 %s 작성자(작성자 정보나 본문으로 확인되는 경우)의 리뷰를 우선 반영하세요.
                  다른 성별의 체형에서만 의미 있는 내용(예: 여성 66 사이즈에 잘 맞았다, 남성 105 기준 넉넉하다)은
                  이 고객에게는 빼세요.
                - 성별과 상관없는 상품 특성(소재, 기능, 세탁 등)은 작성자 성별과 관계없이 그대로 반영하세요.
                - 이 고객 기준으로 걸러낸 뒤 남는 내용이 없으면 "관련 언급을 찾을 수 없습니다"라고 답하세요.
                """.formatted(viewer, viewer);
    }

    private static String reviewerLabel(Review review) {
        Gender gender = review.getGenderAtCreation();
        Integer age = review.getAgeAtCreation();
        if (gender == null && age == null) {
            return "정보 없음";
        }
        String genderText = gender != null ? genderLabel(gender) : "성별 모름";
        String ageText = age != null ? (age / 10 * 10) + "대" : "연령 모름";
        return genderText + " " + ageText;
    }

    private static String genderLabel(Gender gender) {
        return gender == Gender.MALE ? "남성" : "여성";
    }
}
