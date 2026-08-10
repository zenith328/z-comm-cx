package com.zcommcx.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사이트(z-comm-cx) 자체에 로그인할 수 있는 Google 이메일 허용 목록.
 * {@code email}에는 전체 이메일("zenith@g1project.net") 또는 "@도메인"
 * ("@g1project.net", 해당 도메인 전체 허용) 둘 다 넣을 수 있다 — 확인 순서는
 * {@code SiteAuthService#isAllowed} 참고. 관리 UI는 없고, 지금은 psql로 직접 insert/delete한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "allowed_google_account")
public class AllowedAccount {

    @Id
    private String email;

    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AllowedAccount(String email, String note) {
        this.email = email;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }
}
