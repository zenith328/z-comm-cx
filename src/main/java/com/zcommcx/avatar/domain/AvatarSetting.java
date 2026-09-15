package com.zcommcx.avatar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 쇼호스트 아바타 이미지 설정. 상품별이 아니라 사이트 전체가 공유하는 정지 이미지 1장이라,
 * 고정된 id 하나만 갖는 싱글턴 행으로 관리한다(움직이는 아바타/상품별 아바타는 후속 과제).
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "avatar_setting")
public class AvatarSetting {

    public static final String SINGLETON_ID = "GLOBAL";

    @Id
    @Column(nullable = false)
    private String id = SINGLETON_ID;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    public AvatarSetting(String imageUrl) {
        this.id = SINGLETON_ID;
        this.imageUrl = imageUrl;
    }

    public void update(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
