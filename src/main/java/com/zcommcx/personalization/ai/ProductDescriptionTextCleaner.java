package com.zcommcx.personalization.ai;

public interface ProductDescriptionTextCleaner {

    /** 웹페이지에서 그대로 긁어온 텍스트 중 상품 설명에 해당하는 부분만 정리해서 반환한다. */
    String clean(String rawText);
}
