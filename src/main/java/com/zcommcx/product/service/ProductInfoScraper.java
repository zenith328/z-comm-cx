package com.zcommcx.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 상세 페이지의 og 메타태그 / JSON-LD(schema.org Product) 구조화 데이터를 읽어
 * 상품명/브랜드/가격/이미지를 추출한다. 대부분의 국내 쇼핑몰(Cafe24 등)이 SEO를 위해
 * 이 두 형식 중 하나는 서버 렌더링으로 제공하므로, JS 실행 없이도 값을 얻을 수 있다.
 */
@Component
public class ProductInfoScraper {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductInfo fetch(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10_000).get();
        } catch (IOException e) {
            throw new IllegalArgumentException("상품 페이지를 불러오지 못했습니다: " + url, e);
        }

        JsonNode productLd = findProductLd(doc);

        String name = textOrNull(productLd, "name");
        if (name == null) name = ogContent(doc, "og:title");
        if (name == null) throw new IllegalArgumentException("상품명을 찾을 수 없습니다: " + url);

        String brand = null;
        if (productLd != null && productLd.has("brand")) {
            brand = textOrNull(productLd.get("brand"), "name");
        }

        Long price = null;
        if (productLd != null && productLd.has("offers")) {
            String priceText = textOrNull(productLd.get("offers"), "price");
            if (priceText != null) {
                try {
                    price = Long.parseLong(priceText.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {
                    // 가격 형식이 다르면 null로 둔다
                }
            }
        }

        List<String> imageUrls = new ArrayList<>();
        if (productLd != null && productLd.has("image")) {
            JsonNode imageNode = productLd.get("image");
            if (imageNode.isArray()) {
                imageNode.forEach(n -> imageUrls.add(n.asText()));
            } else if (imageNode.isTextual()) {
                imageUrls.add(imageNode.asText());
            }
        }
        if (imageUrls.isEmpty()) {
            String ogImage = ogContent(doc, "og:image");
            if (ogImage != null) imageUrls.add(ogImage);
        }

        String productCode = extractProductCode(url);

        return new ProductInfo(url, productCode, name.trim(), brand, price, imageUrls);
    }

    private JsonNode findProductLd(Document doc) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                if (isProductNode(node)) return node;
            } catch (IOException ignored) {
                // 다음 script 태그 계속 탐색
            }
        }
        return null;
    }

    private boolean isProductNode(JsonNode node) {
        JsonNode type = node.get("@type");
        return type != null && "Product".equalsIgnoreCase(type.asText());
    }

    private String ogContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) return null;
        String content = meta.attr("content");
        return content.isBlank() ? null : content;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        String text = node.get(field).asText();
        return text.isBlank() ? null : text;
    }

    private String extractProductCode(String url) {
        String path = URI.create(url).getPath();
        String[] segments = path.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isBlank()) return segments[i];
        }
        throw new IllegalArgumentException("URL에서 상품 코드를 추출할 수 없습니다: " + url);
    }
}
