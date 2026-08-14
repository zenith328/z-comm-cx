package com.zcommcx.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zcommcx.review.service.ReviewService;
import com.zcommcx.review.web.dto.ReviewCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 일부 쇼핑몰 상품 페이지가 리뷰 위젯에 쓰는 비공개(비공식) API를 호출해 실제 구매 리뷰를
 * 가져온다. 지원하는 호스트가 아니면 아예 호출을 시도하지 않으며, API 호출이 실패해도
 * 상품 등록 자체는 계속 진행되도록 실패를 삼킨다 (best-effort 부가 기능).
 *
 * <p>쇼핑몰마다 API 경로/파라미터/인증 방식이 제각각이라 호스트별로 별도 fetch 메서드를 둔다:
 * <ul>
 *   <li>zerogram.co.kr — {@code GET /api/reviews:bulk?productCodes=...} (세션 불필요, 콤마로
 *       여러 상품코드를 합산 조회하는 방식이지만 여기서는 등록된 상품코드 하나만 넘긴다)</li>
 *   <li>bylynn.shop — {@code GET /api/reviews?productCode=...} (먼저 상품 페이지를 한 번 GET해
 *       세션 쿠키를 발급받아야 응답이 오고, 쿠키 없이 호출하면 빈 205 응답만 온다)</li>
 * </ul>
 * 두 API 모두 응답 리뷰 항목의 필드명(`displayed`/`review`/`score`/`reviewImages`/`memberName`)이
 * 동일해서 파싱 로직({@link #parseReviews})은 공유한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalReviewImportService {

    private static final int IMPORT_LIMIT = 10;
    private static final String ZEROGRAM_HOST = "zerogram.co.kr";
    private static final String BYLYNN_HOST = "bylynn.shop";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .build();

    public void importReviews(String sourceUrl, String productCode) {
        String host = URI.create(sourceUrl).getHost();
        if (host == null) return;

        List<ReviewCreateRequest> requests;
        try {
            if (host.endsWith(ZEROGRAM_HOST)) {
                requests = fetchZerogramReviews(sourceUrl, productCode);
            } else if (host.endsWith(BYLYNN_HOST)) {
                requests = fetchBylynnReviews(sourceUrl, productCode);
            } else {
                return;
            }
        } catch (Exception e) {
            log.info("외부 리뷰 가져오기를 건너뜁니다 (API 호출 실패). productCode={}, message={}",
                    productCode, e.getMessage());
            return;
        }

        requests.forEach(reviewService::createExternalReview);
        log.info("외부 리뷰 {}건을 가져왔습니다. productCode={}", requests.size(), productCode);
    }

    private List<ReviewCreateRequest> fetchZerogramReviews(String sourceUrl, String productCode)
            throws IOException, InterruptedException {
        URI source = URI.create(sourceUrl);
        String apiUrl = "%s://%s/api/reviews:bulk?productCodes=%s&offset=0&limit=%d".formatted(
                source.getScheme(),
                source.getHost(),
                URLEncoder.encode(productCode, StandardCharsets.UTF_8),
                IMPORT_LIMIT);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return parseReviews(send(request), productCode);
    }

    private List<ReviewCreateRequest> fetchBylynnReviews(String sourceUrl, String productCode)
            throws IOException, InterruptedException {
        URI source = URI.create(sourceUrl);
        String origin = "%s://%s".formatted(source.getScheme(), source.getHost());

        // 세션 쿠키가 없으면 이 API가 빈 205 응답만 반환하므로, 먼저 상품 페이지를 방문해
        // 쿠키를 발급받는다 (httpClient의 CookieManager가 이후 요청에 자동으로 실어 보낸다).
        HttpRequest primeRequest = HttpRequest.newBuilder(source)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        httpClient.send(primeRequest, HttpResponse.BodyHandlers.discarding());

        String apiUrl = "%s/api/reviews?productCode=%s&offset=0&limit=%d&sort=Recently&filter=".formatted(
                origin, URLEncoder.encode(productCode, StandardCharsets.UTF_8), IMPORT_LIMIT);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Referer", sourceUrl)
                .GET()
                .build();
        return parseReviews(send(request), productCode);
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("리뷰 API 응답 실패. status=" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray()) {
            throw new IOException("리뷰 API 응답 형식이 배열이 아닙니다.");
        }
        return root;
    }

    private List<ReviewCreateRequest> parseReviews(JsonNode root, String productCode) {
        List<ReviewCreateRequest> requests = new ArrayList<>();
        for (JsonNode node : root) {
            if (!node.path("displayed").asBoolean(true)) continue;

            String content = node.path("review").asText("");
            if (content.isBlank()) continue;

            int rating = Math.min(5, Math.max(1, node.path("score").asInt(5)));
            boolean hasPhoto = node.path("reviewImages").isArray() && !node.path("reviewImages").isEmpty();
            String memberId = node.path("memberName").asText("익명");

            requests.add(new ReviewCreateRequest(productCode, memberId, null, content, rating, hasPhoto));
        }
        return requests;
    }
}
