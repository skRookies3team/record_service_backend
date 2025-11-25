package com.example.petlog.controller;

import com.example.petlog.dto.request.UserRequest;
import com.example.petlog.dto.response.UserResponse;
import com.example.petlog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    //회원가입
    @PostMapping("/create")
    public ResponseEntity<UserResponse.CreateUserDto> createUser(@Valid @RequestBody UserRequest.CreateUserDto request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse.LoginDto> login(
            @RequestBody UserRequest.LoginDto loginRequestDto
    ) {
        UserResponse.LoginDto loginResponseDto = userService.login(loginRequestDto);
        return ResponseEntity.ok(loginResponseDto);
    }
}
