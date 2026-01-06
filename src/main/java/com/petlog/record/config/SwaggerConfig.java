package com.petlog.record.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.SecurityRequirement;

/**
 * [API 문서화 설정]
 * OpenAPI 3.0(Swagger UI)을 이용하여 API 명세서를 자동 생성하고
 * 개발 중 API 테스트를 위한 보안 및 서버 환경을 설정하는 클래스
 */
@Configuration
public class SwaggerConfig {

    /**
     * [OpenAPI 핵심 설정 빈 등록]
     * API의 기본 정보, 서버 경로, 그리고 JWT 보안 인증 방식을 정의
     */
    @Bean
    public OpenAPI openAPI() {

        // 보안 스킴 명칭 정의 (UI상에 표시될 인증 방식 이름)
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                /*
                 * [서버 경로 설정]
                 * API 테스트 시 기본이 되는 서버 URL을 설정 (현재 상대 경로 "/" 사용)
                 */
                .addServersItem(new Server().url("/"))

                /*
                 * [API 기본 정보 설정]
                 * 제목, 버전 등 문서 상단에 노출될 메타데이터 정의
                 */
                .info(new Info().title("PetLog API").version("v1.0"))

                /*
                 * [보안 구성(Components) 설정]
                 * Swagger UI에서 JWT 토큰을 입력할 수 있는 'Authorize' 기능을 활성화
                 * HTTP Bearer 방식의 JWT 인증 스킴을 등록
                 */
                // 1. 보안 스킴 등록 (JWT 설정)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))

                /*
                 * [전역 보안 요구사항 적용]
                 * 등록된 보안 스킴(bearerAuth)을 모든 API에 기본적으로 적용하여
                 * 문서상의 모든 API 호출 시 토큰 헤더가 포함되도록 설정
                 */
                // 2. 모든 API에 대해 이 보안 스킴을 기본적으로 적용
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    /**
     * [API 상세 정보 정의]
     * 프로젝트 명칭, 설명, 버전 등 세부 정보를 작성하는 헬퍼 메서드
     */
    private Info apiInfo() {
        return new Info()
                .title("Petlog_Diary API")
                .description("Petlog 프로젝트 Diary API 명세서입니다.")
                .version("1.0.0");
    }
}