package com.zcommcx.personalization.ai;

public interface ProductDescriptionImageExtractor {

    /** 이미지(바이트)에 담긴 상품 설명 텍스트를 추출한다. */
    String extractText(byte[] imageBytes, String mimeType);
}
