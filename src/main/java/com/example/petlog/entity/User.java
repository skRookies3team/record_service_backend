package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.security.auth.Subject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //아이디
    @Column(unique = true, nullable = false)
    private String email;

    //비밀번호
    @Column(nullable = false)
    private String password;

    //암호화된 비밀번호
    @Column(nullable = false)
    private String encryptedPwd;

    //사용자 이름(닉네임)
    @Column(nullable = false)
    private String username;

    //사용자 타입
    @Enumerated(EnumType.STRING)
    private UserType type;

    //성별
    @Enumerated(EnumType.STRING)
    private GenderType genderType;

    //프로필 사진
    @Column(nullable = true)
    private String profileImage;

    //나이
    @Column(nullable = false)
    private Integer age;

    //위도
    @Column(nullable = true)
    private Integer currentLat;

    //경도
    @Column(nullable = true)
    private Integer currentLng;

    //펫코인
    @Column(nullable = false)
    private Long petCoin;

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Pet> pets = new ArrayList<>();



    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;



}
