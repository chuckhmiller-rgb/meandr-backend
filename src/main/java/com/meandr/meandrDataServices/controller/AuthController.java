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

//@CrossOrigin(origins = "https://meandr-app.vercel.app")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String login = body.get("login");    // username or email
        String password = body.get("password");
        String passwordHash = null;

        try {
            // Try username first, then email
            Users user;
            try {
                user = usersService.getApplicationUserByUsername(login);
                System.out.println("found by username: " + user.getUsername());
                System.out.println("passwordHash: " + user.getPasswordHash());
            } catch (RuntimeException e) {
                user = usersService.getApplicationUserByEmail(login);
                System.out.println("found by email: " + user.getUsername());
                System.out.println("passwordHash: " + user.getPasswordHash());
            }
            passwordHash = user.getPasswordHash();
            System.out.println("final passwordHash: " + passwordHash);
            System.out.println("matches: " + passwordEncoder.matches(password, passwordHash));
            if (!passwordEncoder.matches(password, passwordHash)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials"));
            }

            String token = jwtUtil.generateToken(user.getUsername());
            System.out.println("generating token for: " + user.getUsername());
            String responsetoken = jwtUtil.generateToken(user.getUsername());
            System.out.println("response token generated: " + token);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "navigationApp", user.getNavigationApp() != null ? user.getNavigationApp() : "",
                    "navigationVoice", user.getNavigationVoice() != null ? user.getNavigationVoice() : "",
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));

        } catch (RuntimeException e) {
            System.out.println("CAUGHT EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }
    }
}
