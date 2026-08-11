package com.zcommcx.personalization.web;

import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.SegmentKeyword;
import com.zcommcx.personalization.service.SegmentKeywordService;
import com.zcommcx.personalization.web.dto.SegmentKeywordRequest;
import com.zcommcx.personalization.web.dto.SegmentKeywordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 세그먼트(성별×연령)별 키워드는 상품별이 아니라 전체 상품 공통으로 관리한다 — 상품마다
 * 12개 세그먼트를 반복 입력하는 건 비현실적이라, 이 메뉴 하나에서 세그먼트당 한 세트만 관리한다.
 */
@RestController
@RequestMapping("/api/segment-keywords")
@RequiredArgsConstructor
public class SegmentKeywordController {

    private final SegmentKeywordService service;

    @GetMapping
    public List<SegmentKeywordResponse> list() {
        Map<CustomerSegment, SegmentKeyword> bySegment = service.listAll().stream()
                .collect(Collectors.toMap(SegmentKeyword::getSegment, Function.identity()));
        return List.of(CustomerSegment.values()).stream()
                .map(segment -> SegmentKeywordResponse.of(segment, bySegment.get(segment)))
                .toList();
    }

    @PutMapping("/{segment}")
    public SegmentKeywordResponse upsert(@PathVariable CustomerSegment segment, @RequestBody SegmentKeywordRequest request) {
        return SegmentKeywordResponse.of(segment, service.upsert(segment, request.keywords()));
    }
}
