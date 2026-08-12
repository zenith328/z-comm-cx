package com.zcommcx.aiusage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini API 등급(무료/유료 등)과 한도(RPM/TPM/RPD)는 배포 환경마다(로컬 vs Render) 쓰는
 * 계정이 달라 한도도 다르므로 환경변수로 오버라이드할 수 있게 설정값으로 뺐다.
 * 실제로 사용량 대비 초과 여부를 추적/표시하는 건 RPD뿐이고, RPM/TPM은 화면에 참고 표시만 한다.
 */
@ConfigurationProperties(prefix = "gemini.usage")
public record GeminiUsageProperties(String tier, int rpmLimit, int tpmLimit, int rpdLimit) {
}
