package com.zcommcx.avatar.service;

import com.zcommcx.avatar.domain.AvatarSetting;
import com.zcommcx.avatar.domain.AvatarSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 쇼호스트 아바타 이미지(사이트 전체 공통, 정지 이미지 1장) 설정. 상품별 아바타/움직이는
 * 애니메이션은 후속 과제로 남겨두고, 지금은 관리자가 이미지 URL 하나만 등록/교체한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvatarSettingService {

    private final AvatarSettingRepository repository;

    public String getImageUrl() {
        return repository.findById(AvatarSetting.SINGLETON_ID).map(AvatarSetting::getImageUrl).orElse(null);
    }

    @Transactional
    public String updateImageUrl(String imageUrl) {
        AvatarSetting setting = repository.findById(AvatarSetting.SINGLETON_ID)
                .map(existing -> {
                    existing.update(imageUrl);
                    return existing;
                })
                .orElseGet(() -> repository.save(new AvatarSetting(imageUrl)));
        return setting.getImageUrl();
    }
}
