package com.example.petlog.security.jwt;

import com.example.petlog.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserInfoDetails implements UserDetails {

    private String email;
    private String password;
    private String encryptedPwd;
    private List<GrantedAuthority> authorities;
    private User userInfo;
    private Long userId;

    public UserInfoDetails(User userInfo) {
        this.userInfo = userInfo;
        this.email = userInfo.getEmail();
        this.userId = userInfo.getId();
        this.password=userInfo.getPassword();
        this.encryptedPwd = userInfo.getEncryptedPwd();
        this.authorities = Stream.of(userInfo.getType())
                .map(type -> new SimpleGrantedAuthority(type.name()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return encryptedPwd;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public User getUserInfo() {
        return userInfo;
    }

    public Long getUserId() { return userId; }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
