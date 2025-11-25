package com.example.petlog.service;

import com.example.petlog.dto.request.UserRequest;
import com.example.petlog.dto.response.UserResponse;
import com.example.petlog.entity.Pet;
import com.example.petlog.entity.Status;
import com.example.petlog.entity.User;
import com.example.petlog.entity.UserType;
import com.example.petlog.exception.BusinessException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.PetRepository;
import com.example.petlog.repository.UserRepository;
import com.example.petlog.security.jwt.UserInfoDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PetService petService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final Environment env;


    @Override
    // 유저 생성
    public UserResponse.CreateUserDto createUser(UserRequest.CreateUserDto request) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        //아이디 중복
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ID_DUPLICATE);
        }
        //닉네임 중복
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_NAME_DUPLICATE);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .encryptedPwd(encodedPassword)
                .profileImage(request.getProfileImage())
                .genderType(request.getGenderType())
                .petCoin(0L)
                .age(request.getAge())
                .type(UserType.USER)
                .username(request.getUsername())
                .build();
        User savedUser = userRepository.save(user);

        if (request.getPet()!= null) {
            petService.createPet(savedUser.getId(), request.getPet());

        }
        return UserResponse.CreateUserDto.fromEntity(savedUser);
    }


    @Override
    public UserResponse.AuthDto getUserDetailsByUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.AuthDto.fromEntity(user);
    }

    @Override
    public UserResponse.LoginDto login(UserRequest.LoginDto authRequest) {
        // 1. AuthenticationManager를 사용하여 사용자 인증 시도
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword());
        // Spring Security 인증 체인 실행(UserDetails, PasswordEncoder 동작)
        Authentication authentication = authenticationManager.authenticate(
                authToken);
        // 2. 인증 성공 후 토큰 생성 및 응답 DTO 빌드
        if (authentication.isAuthenticated()) {

            String email = ((UserInfoDetails) authentication.getPrincipal()).getUsername();
            UserResponse.AuthDto userDetails = getUserDetailsByUserId(email);
            String token = generateJwtToken(userDetails);
            return UserResponse.LoginDto.fromEntity(token, userDetails);
        } else {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }


    }
    //jwt 토큰 생성 메서드
    private String generateJwtToken(UserResponse.AuthDto userDetails) {
        // 환경 변수에서 Secret Key와 만료 시간 가져오기
        byte[] secretKeyBytes = Objects.requireNonNull(env.getProperty("token.secret")).getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);

        Instant now = Instant.now();
        Long expirationTime = Long.parseLong(Objects.requireNonNull(env.getProperty("token.expiration-time")));
        String userId = String.valueOf(userDetails.getUserId());

        // JWT 토큰 생성
        return Jwts.builder()
                .subject(userId)
                .expiration(Date.from(now.plusMillis(expirationTime)))
                .issuedAt(Date.from(now))
                .signWith(secretKey)
                .compact();
    }

}
