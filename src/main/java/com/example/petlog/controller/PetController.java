package com.example.petlog.controller;

import com.example.petlog.dto.request.PetRequest;
import com.example.petlog.dto.request.UserRequest;
import com.example.petlog.dto.response.PetResponse;
import com.example.petlog.dto.response.UserResponse;
import com.example.petlog.repository.PetRepository;
import com.example.petlog.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    //펫 추가
    @PostMapping("/create")
    public ResponseEntity<PetResponse.CreatePetDto> createPet(@RequestHeader("X-USER-ID") Long userId, @Valid @RequestBody PetRequest.CreatePetDto request) {
        return ResponseEntity.ok(petService.createPet(userId, request));
    }
}
