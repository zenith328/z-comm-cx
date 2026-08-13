package com.zcommcx.personalization.service;

import com.zcommcx.personalization.ai.ProductDescriptionImageExtractor;
import com.zcommcx.personalization.ai.ProductDescriptionTextCleaner;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 관리자가 붙여넣은 URL 하나로 기본 상세설명을 채운다. 이미지 URL이면 비전으로 텍스트를
 * 추출(OCR)하고, 일반 웹페이지 URL이면 본문 텍스트를 긁어와 AI로 상품 설명만 정리해서 반환한다.
 * 어느 쪽인지는 응답 Content-Type으로 판별한다.
 */
@Service
public class ProductDescriptionUrlExtractionService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final int MAX_RAW_TEXT_LENGTH = 8000;

    private final ProductDescriptionImageExtractor imageExtractor;
    private final ProductDescriptionTextCleaner textCleaner;

    public ProductDescriptionUrlExtractionService(
            ProductDescriptionImageExtractor imageExtractor, ProductDescriptionTextCleaner textCleaner) {
        this.imageExtractor = imageExtractor;
        this.textCleaner = textCleaner;
    }

    public String extractFromUrl(String url) {
        Connection.Response response;
        try {
            response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .ignoreContentType(true)
                    .execute();
        } catch (IOException e) {
            throw new IllegalArgumentException("URL을 불러오지 못했습니다. 주소를 확인해주세요: " + url, e);
        }

        String contentType = response.contentType();
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return imageExtractor.extractText(response.bodyAsBytes(), contentType);
        }

        Document doc = Jsoup.parse(response.body(), url);
        String rawText = doc.body() != null ? doc.body().text() : doc.text();
        if (rawText.isBlank()) {
            throw new IllegalArgumentException("페이지에서 텍스트를 찾을 수 없습니다: " + url);
        }
        String truncated = rawText.length() > MAX_RAW_TEXT_LENGTH ? rawText.substring(0, MAX_RAW_TEXT_LENGTH) : rawText;
        return textCleaner.clean(truncated);
    }
}
