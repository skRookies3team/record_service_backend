package com.example.petlog.repository;

import com.example.petlog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    //사용자 아이디로 찾기
    Optional<User> findByEmail(String email);
    //db id로 찾기
    Optional<User> findById(Long userId);
    boolean existsById(Long userId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
}