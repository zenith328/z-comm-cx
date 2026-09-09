package com.zcommcx.member.web;

import com.zcommcx.common.web.dto.PageResponse;
import com.zcommcx.member.domain.Member;
import com.zcommcx.member.service.MemberService;
import com.zcommcx.member.web.dto.MemberChargeRequest;
import com.zcommcx.member.web.dto.MemberLoginRequest;
import com.zcommcx.member.web.dto.MemberLoginResponse;
import com.zcommcx.member.web.dto.MemberProfileUpdateRequest;
import com.zcommcx.member.web.dto.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 운영자 회원관리 목록. */
    @GetMapping
    public PageResponse<MemberResponse> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Page<Member> members = memberService.list(page, size, search);
        return PageResponse.from(members, MemberResponse::from);
    }

    @PostMapping("/login")
    public MemberLoginResponse login(@Valid @RequestBody MemberLoginRequest request) {
        return MemberLoginResponse.from(memberService.login(request.name(), request.phone()));
    }

    @PutMapping("/profile")
    public MemberResponse updateProfile(@Valid @RequestBody MemberProfileUpdateRequest request) {
        return MemberResponse.from(memberService.updateProfile(
                request.name(), request.phone(), request.gender(), request.birthYear(),
                request.heightCm(), request.weightKg()));
    }

    @PostMapping("/cx-pay/charge")
    public MemberResponse charge(@Valid @RequestBody MemberChargeRequest request) {
        return MemberResponse.from(memberService.charge(request.name(), request.phone(), request.amount()));
    }
}
