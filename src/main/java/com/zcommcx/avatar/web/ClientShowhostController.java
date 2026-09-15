package com.zcommcx.avatar.web;

import com.zcommcx.avatar.domain.ShowhostScript;
import com.zcommcx.avatar.service.AvatarSettingService;
import com.zcommcx.avatar.service.ShowhostService;
import com.zcommcx.avatar.web.dto.ShowhostQnaRequest;
import com.zcommcx.avatar.web.dto.ShowhostQnaResponse;
import com.zcommcx.avatar.web.dto.ShowhostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구매자(Client)에게 노출되는 AI 쇼호스트 방송 API. 실제 라이브 스트리밍 서버가 아니라, 접속
 * 시점에 대본을 생성/캐시하고 브라우저가 TTS로 읽어주는 온디맨드 방식이다.
 */
@RestController
@RequestMapping("/api/products/{productCode}/showhost")
@RequiredArgsConstructor
public class ClientShowhostController {

    private final ShowhostService showhostService;
    private final AvatarSettingService avatarSettingService;

    @GetMapping
    public ShowhostResponse get(@PathVariable String productCode) {
        ShowhostScript script = showhostService.getOrGenerate(productCode);
        return ShowhostResponse.of(script, avatarSettingService.getImageUrl());
    }

    @PostMapping("/regenerate")
    public ShowhostResponse regenerate(@PathVariable String productCode) {
        ShowhostScript script = showhostService.regenerate(productCode);
        return ShowhostResponse.of(script, avatarSettingService.getImageUrl());
    }

    @PostMapping("/qna")
    public ShowhostQnaResponse ask(@PathVariable String productCode, @Valid @RequestBody ShowhostQnaRequest request) {
        return new ShowhostQnaResponse(showhostService.answerQuestion(productCode, request.question()));
    }
}
