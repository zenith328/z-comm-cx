package com.zcommcx.personalization.ai;

import com.zcommcx.personalization.domain.CustomerSegment;

import java.util.List;

public interface SegmentKeywordSuggester {

    /**
     * 해당 세그먼트 고객이 쓴 것으로 확인된 리뷰 발췌를 분석해, 상세설명 작성 시 참고할 만한
     * 키워드 후보를 제안한다. reviewExcerpts가 비어있으면 호출하지 않는 것이 호출자의 책임이다.
     */
    List<String> suggest(CustomerSegment segment, List<String> reviewExcerpts, String existingKeywords);
}
