package com.meandr.meandrDataServices.service;

import com.meandr.meandrDataServices.model.PasswordResetToken;
import com.meandr.meandrDataServices.model.Users;
import com.meandr.meandrDataServices.repository.PasswordResetTokenRepository;
import com.meandr.meandrDataServices.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UsersRepository usersRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.base-url}")
    private String baseUrl;

    @Transactional
    public void requestReset(String email) {
        Optional<Users> userOpt = usersRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal whether the email exists — always behave the same way
            log.info("Password reset requested for unknown email: {}", email);
            return;
        }

        Users user = userOpt.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        //String resetLink = baseUrl + "/reset-password?token=" + token;
        String resetLink = baseUrl + "/?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset your meandr password");
        message.setText("Someone requested a password reset for your meandr account.\n\n"
                + "Click the link below to set a new password (expires in 1 hour):\n"
                + resetLink + "\n\n"
                + "If you didn't request this, you can safely ignore this email.");
        mailSender.send(message);

        log.info("Password reset email sent to user id={}", user.getId());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        Optional<Users> userOpt = usersRepository.findById(resetToken.getUserId());
        if (userOpt.isEmpty()) return false;

        Users user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }
}