package com.zcommcx.personalization.web.dto;

import com.zcommcx.personalization.service.ResolvedDescription;

public record ProductDescriptionResponse(String text, boolean personalized) {

    public static ProductDescriptionResponse from(ResolvedDescription resolved) {
        return new ProductDescriptionResponse(resolved.text(), resolved.personalized());
    }
}
