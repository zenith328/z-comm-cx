package com.zcommcx.auth.web;

import com.zcommcx.auth.config.GoogleAuthProperties;
import com.zcommcx.auth.service.SiteAuthService;
import com.zcommcx.auth.web.dto.GoogleLoginRequest;
import com.zcommcx.auth.web.dto.SiteAuthConfigResponse;
import com.zcommcx.auth.web.dto.SiteAuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SiteAuthController {

    public static final String SESSION_EMAIL_KEY = "siteAuthEmail";

    private final SiteAuthService siteAuthService;
    private final GoogleAuthProperties properties;

    /** 프론트가 Google Identity Services를 초기화할 때 쓸 클라이언트 ID (비밀값 아님). */
    @GetMapping("/config")
    public SiteAuthConfigResponse config() {
        return new SiteAuthConfigResponse(properties.clientId());
    }

    @PostMapping("/google")
    public SiteAuthResponse login(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        String email = siteAuthService.authenticate(request.idToken());
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SESSION_EMAIL_KEY, email);
        return new SiteAuthResponse(email);
    }

    @GetMapping("/me")
    public SiteAuthResponse me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String email = session != null ? (String) session.getAttribute(SESSION_EMAIL_KEY) : null;
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return new SiteAuthResponse(email);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
