package com.zcommcx.personalization.domain;

import com.zcommcx.member.domain.Gender;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 상품 상세설명을 성향별로 다르게 보여주기 위한 고객 세그먼트.
 * 세그먼트가 늘어날수록 AI 생성 비용/관리 부담이 커지므로, 성별×연령을 3구간(10~20대/30~40대/
 * 50대 이상)으로 묶어 총 6종만 둔다. 추후 즐겨찾기/구매이력/선호카테고리 기반 세그먼트로 확장할 수 있다.
 */
@Getter
public enum CustomerSegment {
    MALE_10_20S(Gender.MALE, 10, 29, "남성 10~20대"),
    MALE_30_40S(Gender.MALE, 30, 49, "남성 30~40대"),
    MALE_50S_PLUS(Gender.MALE, 50, null, "남성 50대 이상"),
    FEMALE_10_20S(Gender.FEMALE, 10, 29, "여성 10~20대"),
    FEMALE_30_40S(Gender.FEMALE, 30, 49, "여성 30~40대"),
    FEMALE_50S_PLUS(Gender.FEMALE, 50, null, "여성 50대 이상");

    private final Gender gender;
    private final Integer minAge;
    private final Integer maxAge;
    private final String label;

    CustomerSegment(Gender gender, Integer minAge, Integer maxAge, String label) {
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.label = label;
    }

    /** 성별+연령이 어느 세그먼트에 해당하는지 찾는다. 둘 중 하나라도 없으면(미입력) 매칭하지 않는다. */
    public static Optional<CustomerSegment> forGenderAndAge(Gender gender, Integer age) {
        if (gender == null || age == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(segment -> segment.gender == gender)
                .filter(segment -> age >= segment.minAge && (segment.maxAge == null || age <= segment.maxAge))
                .findFirst();
    }
}
