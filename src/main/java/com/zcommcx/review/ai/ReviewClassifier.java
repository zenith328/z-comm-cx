package com.zcommcx.review.ai;

import com.zcommcx.review.domain.Review;

public interface ReviewClassifier {

    ReviewAiResult classify(Review review);
}
