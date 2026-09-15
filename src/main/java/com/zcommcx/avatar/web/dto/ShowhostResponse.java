package com.zcommcx.avatar.web.dto;

import com.zcommcx.avatar.domain.ShowhostScript;

import java.time.LocalDateTime;

public record ShowhostResponse(String script, String avatarImageUrl, LocalDateTime generatedAt) {

    public static ShowhostResponse of(ShowhostScript script, String avatarImageUrl) {
        return new ShowhostResponse(script.getScript(), avatarImageUrl, script.getGeneratedAt());
    }
}
