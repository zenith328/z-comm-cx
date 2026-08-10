package com.zcommcx.auth.service;

import com.zcommcx.auth.domain.AllowedAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SiteAuthService {

    private final GoogleTokenVerifier tokenVerifier;
    private final AllowedAccountRepository allowedAccountRepository;

    /**
     * Google ID 토큰을 검증하고, 허용 목록(allowed_google_account)에 있는 이메일인지 확인한다.
     * 허용되지 않은 이메일이면 로그인 자체(토큰 검증)는 성공해도 사이트 접근은 막는다.
     */
    public String authenticate(String idToken) {
        GoogleIdentity identity = tokenVerifier.verify(idToken);
        String email = identity.email();
        if (!isAllowed(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "허용되지 않은 계정입니다. (%s)".formatted(email));
        }
        return email;
    }

    /**
     * 1) 이메일 전체가 허용 목록에 있는지 먼저 확인하고, 2) 없으면 "@도메인" 형태의 허용 목록
     * 항목이 있는지 확인한다 (예: allowed_google_account에 "@g1project.net"을 넣어두면 그
     * 도메인의 모든 이메일이 통과).
     */
    private boolean isAllowed(String email) {
        if (allowedAccountRepository.existsById(email)) {
            return true;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return false;
        }
        String domainEntry = email.substring(atIndex);
        return allowedAccountRepository.existsById(domainEntry);
    }
}
