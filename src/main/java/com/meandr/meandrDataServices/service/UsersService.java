/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.service;

import com.meandr.meandrDataServices.dto.UsersRegistrationDto;
import com.meandr.meandrDataServices.dto.UsersResponseDto;
import com.meandr.meandrDataServices.model.Users;
import com.meandr.meandrDataServices.dto.UsersUpdateDto;
import com.meandr.meandrDataServices.model.UserStatus;
import com.meandr.meandrDataServices.mapper.UsersFieldUpdateMapper;
import com.meandr.meandrDataServices.util.Patcher;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.meandr.meandrDataServices.repository.UsersRepository;
import com.meandr.meandrDataServices.mapper.UsersModelMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UsersService {

    @Autowired
    private final UsersFieldUpdateMapper updateMapper;
    private final UsersRepository applicationUsersRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository userRepository;
    private final UsersModelMapper userMapper; // Injected MapStruct mapper
    private final Patcher userPatcher;
    private final EntityPreferenceService entityPreferenceService;

    //private final PasswordEncoder passwordEncoder;
    public List<UsersResponseDto> getAllUsers() {
        return applicationUsersRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();

    }

    public Users getApplicationUserByUsername(String username) {
        return applicationUsersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public Users getApplicationUserByEmail(String email) {
        return applicationUsersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public boolean existsByUsername(String username) {
        return applicationUsersRepository.findByUsername(username).isPresent();
    }

    @Transactional
    public Users createUser(UsersRegistrationDto dto) {
        // 1. Check for duplicates
        if (applicationUsersRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (applicationUsersRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // 2. Map DTO to Entity & Hash Password
        Users user = Users.builder()
                .navigationApp(dto.getNavigationApp())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword())) // Hashing
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .displayName(dto.getDisplayName())
                .phone(dto.getPhone())
                .countryCode(dto.getCountryCode())
                .avatarUrl(dto.getAvatarUrl())
                .bio(dto.getBio())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        // 3. Persist
        Users saved = applicationUsersRepository.save(user);

        // 4. Save entity preferences if provided
        if (dto.getEntityPreferences() != null && !dto.getEntityPreferences().isEmpty()) {
            entityPreferenceService.savePreferences(saved.getId(), dto.getEntityPreferences());
        }

        return saved;

    }

    @Transactional
    public Users updateUser(String userName, UsersUpdateDto dto) {
        // 1. Fetch
        Users existingUser = applicationUsersRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 2. Map (MapStruct handles the null checks internally)
        userMapper.updateEntityFromDto(dto, existingUser);

        // 3. Save
        return userRepository.save(existingUser);
    }

    @Transactional
    public Users updateFields(String userName, UsersUpdateDto dto) {
        // 1. Get the existing entity from DB
        Users existingUser = userRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException());

        // 2. Map only non-null fields from DTO to the Entity
        updateMapper.updateEntityFromDto(dto, existingUser);

        // 3. Save the modified entity
        return userRepository.save(existingUser);
    }

    @Transactional
    public void applyPatch(String userName, Map<String, Object> updates) {
        Users existingUser = userRepository.findByUsername(userName)
                .orElseThrow(() -> new EntityNotFoundException());

        userPatcher.applyPatch(existingUser, updates);
    }

    private UsersResponseDto toResponseDTO(Users applicationUsers) {
        return UsersResponseDto.builder()
                .username(applicationUsers.getUsername())
                .email(applicationUsers.getEmail())
                .firstName(applicationUsers.getFirstName())
                .lastName(applicationUsers.getLastName())
                .displayName(applicationUsers.getDisplayName())
                .phone(applicationUsers.getPhone())
                .countryCode(applicationUsers.getCountryCode())
                .status(applicationUsers.getStatus().toString())
                .avatarUrl(applicationUsers.getAvatarUrl())
                .bio(applicationUsers.getBio())
                .lastLoginAt(applicationUsers.getLastLoginAt())
                .createdAt(applicationUsers.getCreatedAt())
                .updatedAt(applicationUsers.getUpdatedAt())
                .build();
    }

}
