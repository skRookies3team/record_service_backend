package com.example.petlog.service;

import com.example.petlog.dto.request.PetRequest;
import com.example.petlog.dto.response.PetResponse;

public interface PetService {

    PetResponse.CreatePetDto createPet(Long userNum, PetRequest.CreatePetDto request);
}
