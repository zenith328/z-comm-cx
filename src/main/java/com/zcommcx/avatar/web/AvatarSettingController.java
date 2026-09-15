package com.zcommcx.avatar.web;

import com.zcommcx.avatar.service.AvatarSettingService;
import com.zcommcx.avatar.web.dto.AvatarSettingRequest;
import com.zcommcx.avatar.web.dto.AvatarSettingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 쇼호스트 아바타 이미지는 상품별이 아니라 사이트 전체 공통이라 segment-keywords와 같은
 * 관례로 /api/admin 접두사 없이 별도 엔드포인트를 둔다(이 저장소는 관리자 화면 접근을
 * 프론트엔드 라우팅으로만 구분하고, 별도 서버측 인가는 두지 않는다).
 */
@RestController
@RequestMapping("/api/avatar-showhost/setting")
@RequiredArgsConstructor
public class AvatarSettingController {

    private final AvatarSettingService avatarSettingService;

    @GetMapping
    public AvatarSettingResponse get() {
        return new AvatarSettingResponse(avatarSettingService.getImageUrl());
    }

    @PutMapping
    public AvatarSettingResponse update(@Valid @RequestBody AvatarSettingRequest request) {
        return new AvatarSettingResponse(avatarSettingService.updateImageUrl(request.imageUrl()));
    }
}
