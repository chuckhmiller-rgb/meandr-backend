/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.dto;

//import com.meandr.entity.Users.UserStatus;
import com.meandr.meandrDataServices.model.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersResponseDto{
        //Long id;
        String navigationApp;
        String username;
        String email;
        String firstName;
        String lastName;
        String displayName;
        String phone;
        String countryCode;
        String status;
        boolean emailVerified;
        boolean phoneVerified;
        String avatarUrl;
        String bio;
        LocalDateTime lastLoginAt;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

    // You can add static factory method if you prefer
    public static UsersResponseDto fromEntity(Users user) {
        return new UsersResponseDto(
                //user.getApplication_user_id(),
                user.getNavigationApp(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getPhone(),
                user.getCountryCode(),
                user.getStatus().toString(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public String id() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public String getId() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
