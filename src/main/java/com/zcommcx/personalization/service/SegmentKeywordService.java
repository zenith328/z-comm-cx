package com.zcommcx.personalization.service;

import com.zcommcx.personalization.domain.CustomerSegment;
import com.zcommcx.personalization.domain.SegmentKeyword;
import com.zcommcx.personalization.domain.SegmentKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SegmentKeywordService {

    private final SegmentKeywordRepository repository;

    public List<SegmentKeyword> listAll() {
        return repository.findAll();
    }

    @Transactional
    public SegmentKeyword upsert(CustomerSegment segment, String keywords) {
        return repository.findById(segment)
                .map(existing -> {
                    existing.updateKeywords(keywords);
                    return existing;
                })
                .orElseGet(() -> repository.save(new SegmentKeyword(segment, keywords)));
    }

    public String getKeywords(CustomerSegment segment) {
        return repository.findById(segment).map(SegmentKeyword::getKeywords).orElse(null);
    }
}
