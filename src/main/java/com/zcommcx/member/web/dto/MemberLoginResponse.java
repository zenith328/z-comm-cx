package com.zcommcx.member.web.dto;

import com.zcommcx.member.service.MemberLoginResult;

public record MemberLoginResponse(MemberResponse member, boolean firstLogin) {

    public static MemberLoginResponse from(MemberLoginResult result) {
        return new MemberLoginResponse(MemberResponse.from(result.member()), result.firstLogin());
    }
}
