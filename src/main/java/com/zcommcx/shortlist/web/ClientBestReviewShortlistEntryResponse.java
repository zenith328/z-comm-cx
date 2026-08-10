package com.zcommcx.shortlist.web;

import com.zcommcx.review.web.dto.ClientReviewResponse;

/**
 * 구매자 노출용 숏리스트 항목. productCode는 조회 시점에 이미 알고 있으므로 생략하고,
 * 리뷰도 운영 내부 정보가 없는 {@link ClientReviewResponse}로 감싼다.
 */
public record ClientBestReviewShortlistEntryResponse(String weekLabel, int rank, ClientReviewResponse review) {
}
