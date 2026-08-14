package com.zcommcx.personalization.ai;

import com.zcommcx.personalization.domain.CustomerSegment;

public interface ProductDescriptionGenerator {

    /**
     * 기본 상세설명과 세그먼트별 키워드(관리자 입력, 없을 수 있음)를 참고해
     * 해당 세그먼트에 맞는 상세설명 본문을 생성하고, AI 스스로 적합도를 평가한다.
     */
    GeneratedDescription generate(String productName, String brand, String baseDescription, CustomerSegment segment, String keywords);
}
