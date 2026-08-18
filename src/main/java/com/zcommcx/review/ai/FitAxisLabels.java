package com.zcommcx.review.ai;

/**
 * 핏 가이드가 보여주는 3개 축의 이름은 상품 카테고리에 따라 달라야 한다 — "어깨/가슴/기장"은
 * 상의·아우터에나 맞는 표현이고, 신발/하의에 그대로 쓰면 어색하다(예: 신발인데 "어깨"가 나옴).
 * 실제 핏 판정 값(FitLevel: TIGHT/TRUE_TO_SIZE/LOOSE/UNKNOWN)의 의미는 그대로 두고, 그 값이
 * "무엇에 대한" 판정인지 라벨만 카테고리별로 바꿔치기한다. 카테고리 텍스트는 자유 입력이라
 * 완벽한 분류는 못 하므로, 키워드 매칭에 실패하면 상의 기준(가장 흔한 케이스)으로 대체한다.
 */
public final class FitAxisLabels {

    private FitAxisLabels() {
    }

    public record Labels(String axis1, String axis2, String axis3) {
    }

    private static final Labels TOPS = new Labels("어깨", "가슴", "기장");
    private static final Labels BOTTOMS = new Labels("허리", "밑위", "기장");
    private static final Labels SHOES = new Labels("발볼", "사이즈", "발등");

    public static Labels forCategory(String category) {
        if (category == null || category.isBlank()) {
            return TOPS;
        }
        if (containsAny(category, "신발", "슈즈", "운동화", "샌들", "부츠", "구두")) {
            return SHOES;
        }
        if (containsAny(category, "하의", "팬츠", "바지", "스커트", "치마", "숏츠", "쇼츠")) {
            return BOTTOMS;
        }
        return TOPS;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
