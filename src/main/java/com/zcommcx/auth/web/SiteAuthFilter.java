package com.zcommcx.auth.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * /api/** 전체를 사이트 로그인(Google) 세션으로 막는다. /api/auth/**는 로그인 흐름 자체이므로
 * 예외로 통과시킨다. 이 필터를 통과한 뒤에도 FO 회원 로그인(review write/order/chat)은
 * 프론트 라우터 가드가 별도로 검사한다 — 이 필터는 "사이트에 들어올 자격"만 본다.
 */
public class SiteAuthFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (request.getRequestURI().startsWith("/api/auth/")) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        Object email = session != null ? session.getAttribute(SiteAuthController.SESSION_EMAIL_KEY) : null;
        if (email == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
            return;
        }

        chain.doFilter(req, res);
    }
}
