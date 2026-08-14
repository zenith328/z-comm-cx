package com.zcommcx.member.web;

import com.zcommcx.member.service.MemberService;
import com.zcommcx.member.web.dto.MemberLoginRequest;
import com.zcommcx.member.web.dto.MemberLoginResponse;
import com.zcommcx.member.web.dto.MemberProfileUpdateRequest;
import com.zcommcx.member.web.dto.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/login")
    public MemberLoginResponse login(@Valid @RequestBody MemberLoginRequest request) {
        return MemberLoginResponse.from(memberService.login(request.name(), request.phone()));
    }

    @PutMapping("/profile")
    public MemberResponse updateProfile(@Valid @RequestBody MemberProfileUpdateRequest request) {
        return MemberResponse.from(memberService.updateProfile(
                request.name(), request.phone(), request.gender(), request.birthYear()));
    }
}
