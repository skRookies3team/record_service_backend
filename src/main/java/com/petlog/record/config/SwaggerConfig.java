package com.petlog.record.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {


        // 보안 스킴 이름 (예: "bearerAuth")
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(new Info().title("PetLog API").version("v1.0"))
                // 1. 보안 스킴 등록 (JWT 설정)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // 2. 모든 API에 대해 이 보안 스킴을 기본적으로 적용
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    private Info apiInfo() {
        return new Info()
                .title("Petlog_Diary API")
                .description("Petlog 프로젝트 Diary API 명세서입니다.")
                .version("1.0.0");
    }
}