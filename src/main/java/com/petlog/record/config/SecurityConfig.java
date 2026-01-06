package com.petlog.record.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * [서비스 보안 설정]
     * Spring Security를 필터 체인을 통해 요청 인가 및 인증 정책을 정의하는 클래스
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                 * [상태를 저장하지 않는 API 정책 설정]
                 * REST API 환경에서 세션을 사용하지 않으므로 CSRF 방어 기능을 비활성화하고,
                 * 필요에 따라 CORS 설정을 별도로 관리하기 위해 비활성화함
                 */
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())

                /*
                 * [인증 방식 커스텀 설정]
                 * 기본 제공되는 Form 로그인 및 HTTP Basic 인증창을 사용하지 않음 (JWT 방식 지향)
                 */
                // Form 로그인, Basic 인증 해제 (JWT)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                /*
                 * [경로별 접근 제어(Authorization) 설정]
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * [인프라 및 시스템 모니터링 허용]
                         * Kubernetes의 Liveness/Readiness 프로브 및 Actuator 헬스체크 경로 허용
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        /*
                         * [API 문서화 도구 허용]
                         * 개발 생산성을 위해 Swagger UI 및 OpenAPI 관련 문서는 인증 없이 접근 허용
                         */
                        .requestMatchers(
                                "/swagger",
                                "/swagger/",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        /*
                         * [전체 경로 접근 정책]
                         * 현재 개발 단계의 편의를 위해 모든 요청을 허용하고 있으나,
                         * 운영 단계에서는 .anyRequest().authenticated()로 전환하여 보안을 강화할 예정
                         */
                        // 개발 초기 : 아래처럼 다 열어두고 시작
                        // .anyRequest().authenticated() // (개발 후: 나머지는 인증 필요)
                        .anyRequest().permitAll()      // (개발 편의상: 일단 다 허용)
                );

        return http.build();
    }
}