package com.zcommcx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 프로덕션 배포 시 Vue 빌드 결과물(client/dist)이 이 프로젝트의 정적 리소스로 함께
 * 패키징된다(배포용 Dockerfile 참고, 로컬 개발 시에는 Vite dev server를 그대로 사용하므로
 * 영향 없음). Vue Router가 history 모드(createWebHistory)라서 "/products/1"처럼 실제
 * 정적 파일이 없는 클라이언트 라우트로 직접 접근하면 기본 정적 리소스 핸들러는 404를
 * 반환한다 — 이를 막기 위해 요청한 파일이 존재하지 않을 때 index.html로 대신 응답해
 * Vue Router가 클라이언트에서 라우팅을 이어받도록 한다.
 *
 * "/api/**"는 별도의 @RestController가 먼저 처리하므로 이 설정과 겹치지 않는다.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
