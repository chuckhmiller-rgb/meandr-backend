package com.meandr.meandrDataServices.controller;

import com.meandr.meandrDataServices.model.Users;
import com.meandr.meandrDataServices.security.JwtUtil;
import com.meandr.meandrDataServices.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.lang.ArithmeticException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String login    = body.get("login");    // username or email
        String password = body.get("password");

        try {
            // Try username first, then email
            Users user;
            try {
                user = usersService.getApplicationUserByUsername(login);
            } catch (RuntimeException e) {
                user = usersService.getApplicationUserByEmail(login);
            }

            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
            }

            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(Map.of(
                "token",       token,
                "username",    user.getUsername(),
                "email",       user.getEmail(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                "avatarUrl",   user.getAvatarUrl()   != null ? user.getAvatarUrl()   : ""
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
        }
    }
}