package com.example.petlog.security;

import com.example.petlog.security.jwt.UserInfoDetailsService;
import com.example.petlog.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ALLOWED_IP_ADDRESS = "127.0.0.1";
    public static final String SUBNET = "/32";
    public static final IpAddressMatcher ALLOWED_IP_ADDRESS_MATCHER = new IpAddressMatcher(ALLOWED_IP_ADDRESS + SUBNET);

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        //Rest api 기반의 jwt 인증에서는 csrf 토큰을 사용하지 않으므로 csrf 방어를 비활성화.
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())

                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인 허용
                        .requestMatchers("/api/users/create").permitAll()
                        .requestMatchers("/api/users/login").permitAll()

                        // Swagger 허용
                        .requestMatchers(
                                "/swagger",
                                "/swagger/",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 정적 이미지 리소스 허용
                        .requestMatchers("/products/images/**").permitAll()
                        //인가 설정: 모든 경로에 대한 접근을 특정 ip 주소에서 온 요청으로만 제한한다.
                        //hasIpAddress(): 요청을 보낸 클라이언트의 ip 주소가 지정된 주소와 일치하는지 확인한다.
                        //(127.0.0.1은 localhost, ::1은 IPv6의 localhost를 의미한다.
                        .requestMatchers("/**").access(
                                new WebExpressionAuthorizationManager(
                                        "hasIpAddress('127.0.0.1') or hasIpAddress('::1') or " +
                                                "hasIpAddress('10.90.26.89') or hasIpAddress('::1')")) // host pc ip address
                        //그 외 모든 API 요청은 JWT 인증 필수
                        .anyRequest().authenticated())
                        //Http Basic 인증을 security 필터 체인에 추가한다.
                        //Customer.withDefaults()는 Spring security의 기본 basic 인증을 사용하겠다는 의미이다.
                        //클라이언트가 api 요청을 보낼때 authorization 헤더에
                        //[Base64인코딩된 아이디: 비밀번호] 형식으로 인증 정보를 담아 보내면
                        //Spring Security가 이를 자동으로 해석하고 인증을 시도한다.
                        .httpBasic(Customizer.withDefaults());  // ← Basic 인증 추가



        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }



}
