/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

//import com.meandr.entity.Users.UserStatus;
import com.meandr.meandrDataServices.model.Users;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.util.List;
import com.meandr.meandrDataServices.service.EntityPreferenceService.PreferenceEntry;

// For creation / registration
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersRegistrationDto {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NonNull
    @Email
    private String email;

    @NonNull
    @Size(min = 8)
    private String password;           // ← plain text → will be hashed

    private String firstName;
    private String lastName;
    private String displayName;
    private String phone;
    private String countryCode;
    private String avatarUrl;
    private String bio;
    private List<PreferenceEntry> entityPreferences;

   

}
