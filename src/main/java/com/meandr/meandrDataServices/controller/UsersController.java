/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.controller;

//import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.URI;
import com.meandr.meandrDataServices.dto.UsersRegistrationDto;
import com.meandr.meandrDataServices.dto.UsersResponseDto;
import com.meandr.meandrDataServices.dto.UsersUpdateDto;
import com.meandr.meandrDataServices.model.Users;
import com.meandr.meandrDataServices.preferences.PreferenceTier;
import com.meandr.meandrDataServices.repository.UsersRepository;
import com.meandr.meandrDataServices.security.JwtUtil;
import com.meandr.meandrDataServices.service.EntityPreferenceService;
import com.meandr.meandrDataServices.service.UsersService;
import com.meandr.meandrDataServices.util.Patcher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {

    //private UsersService userService;
    private final UsersService userService;
    private final UsersRepository userRepository;
    private final EntityPreferenceService entityPreferenceService;
    @Autowired
    private Patcher patcher;
    @Autowired
    private JwtUtil jwtUtil;

    /*@GetMapping("/getAllUsers")
    public ResponseEntity<List<UsersResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }*/
    @GetMapping("/exists/{username}")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    /*Optional: Get current authenticated user
    @GetMapping("/me")
    public ResponseEntity<ApplicationUsersDTO> getCurrentUser() {
        Users currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(mapToDTO(currentUser));
    }*/
    @GetMapping("/username/{username}")
    public ResponseEntity<Users> getByUsername(String username) {
        return ResponseEntity.ok(userService.getApplicationUserByUsername(username));

    }

    @PostMapping("/register")
    public ResponseEntity<Users> registerUser(@Valid @RequestBody UsersRegistrationDto registrationDto) {
        Users createdUser = userService.createUser(registrationDto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/model/{userName}")
    public ResponseEntity<Users> updateUser(
            @PathVariable String userName,
            @RequestBody UsersUpdateDto updateDto) {

        Users updatedUser = userService.updateUser(userName, updateDto);
        return ResponseEntity.ok(updatedUser);
    }

    /*@PutMapping("/field/{userName}")
    public ResponseEntity<Users> updateUserField(
            @PathVariable String userName,
            @RequestBody UsersUpdateDto updateDto) {

        Users updatedUser = userService.updateUser(userName, updateDto);
        return ResponseEntity.ok(updatedUser);
    }*/
    @PatchMapping("/patch/{userName}")
    public ResponseEntity<Users> updateFields(@PathVariable String userName, @RequestBody Map<String, Object> updates) {
        log.info("Patch updates for {}: {}", userName, updates);  // ← add this
        Users user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        patcher.applyPatch(user, updates);
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<Users> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Users user = userService.getApplicationUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{userName}/preferences")
    public ResponseEntity<Void> savePreferences(
            @PathVariable String userName,
            @RequestBody Map<String, String> prefsMap) {
        Users user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<EntityPreferenceService.PreferenceEntry> entries = prefsMap.entrySet().stream()
                .map(e -> new EntityPreferenceService.PreferenceEntry(
                e.getKey(),
                PreferenceTier.valueOf(e.getValue())
        ))
                .collect(Collectors.toList());
        entityPreferenceService.savePreferences(user.getId(), entries);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userName}/preferences")
    public ResponseEntity<Map<String, String>> getPreferences(@PathVariable String userName) {
        Users user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, String> prefsMap = entityPreferenceService.loadPreferences(user.getId())
                .stream()
                .collect(Collectors.toMap(
                        EntityPreferenceService.PreferenceEntry::getEntityTypeId,
                        e -> e.getTier().name()
                ));
        return ResponseEntity.ok(prefsMap);
    }

    /*@DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.softDelete(id);
    }*/
}
