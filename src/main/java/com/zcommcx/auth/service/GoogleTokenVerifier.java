package com.zcommcx.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zcommcx.auth.config.GoogleAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * 프론트에서 Google Identity Services로 받은 ID 토큰을 Google의 tokeninfo 엔드포인트로
 * 검증한다. 서명/만료 검사는 Google이 대신 해주므로 여기서는 aud(클라이언트 ID 일치)와
 * email_verified만 추가로 확인한다.
 */
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final GoogleAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public GoogleIdentity verify(String idToken) {
        String url = TOKENINFO_URL + URLEncoder.encode(idToken, StandardCharsets.UTF_8);

        JsonNode node;
        try {
            String raw = restClient.get().uri(url).retrieve().body(String.class);
            node = objectMapper.readTree(raw);
        } catch (RestClientException | java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google 토큰이 유효하지 않습니다.", e);
        }

        String aud = node.path("aud").asText("");
        if (properties.clientId() == null || !properties.clientId().equals(aud)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "클라이언트 ID가 일치하지 않는 토큰입니다.");
        }

        boolean emailVerified = node.path("email_verified").asBoolean(false);
        String email = node.path("email").asText(null);
        if (!emailVerified || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일이 확인되지 않은 계정입니다.");
        }

        return new GoogleIdentity(email);
    }
}
