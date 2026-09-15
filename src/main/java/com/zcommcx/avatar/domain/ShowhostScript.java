package com.zcommcx.avatar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품별 AI 쇼호스트 방송 대본 캐시(상품당 1건). 접속할 때마다 새로 생성하면 비용이 커지므로
 * 한 번 생성한 대본을 저장해두고, 관리자/고객이 "다시 생성"을 누르면 그때만 새로 만든다.
 * 실제 24시간 라이브 스트리밍이 아니라 온디맨드(접속 시점에 생성/재생) 방식이라, 서버는 이
 * 텍스트 대본만 만들고 음성 합성(TTS)은 브라우저가 담당한다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "showhost_script")
public class ShowhostScript {

    @Id
    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(nullable = false, columnDefinition = "text")
    private String script;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public ShowhostScript(String productCode, String script) {
        this.productCode = productCode;
        this.script = script;
        this.generatedAt = LocalDateTime.now();
    }

    public void update(String script) {
        this.script = script;
        this.generatedAt = LocalDateTime.now();
    }
}
