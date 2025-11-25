package com.example.petlog.service;

import com.example.petlog.dto.request.PetRequest;
import com.example.petlog.dto.response.PetResponse;
import com.example.petlog.entity.Pet;
import com.example.petlog.entity.Status;
import com.example.petlog.entity.User;
import com.example.petlog.exception.BusinessException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.PetRepository;
import com.example.petlog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;

    @Override
    // 펫 생성
    public PetResponse.CreatePetDto createPet(Long userId, PetRequest.CreatePetDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //펫 이름 중복
        if (petRepository.existsByUserIdAndPetName(userId, request.getPetName())) {
            throw new BusinessException(ErrorCode.USER_ID_DUPLICATE);
        }

        Pet pet = Pet.builder()
                .user(user)
                .petName(request.getPetName())
                .profileImage(request.getProfileImage())
                .genderType(request.getGenderType())
                .birth(request.getBirth())
                .breed(request.getBreed())
                .is_neutered(request.isNeutered())
                .age(request.getAge())
                .species(request.getSpecies())
                .status(Status.ACTIVE)
                .build();
        Pet savedPet = petRepository.save(pet);
        return PetResponse.CreatePetDto.fromEntity(savedPet);


    }
}
