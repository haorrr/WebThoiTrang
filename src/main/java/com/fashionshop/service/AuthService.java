package com.fashionshop.service;

import com.fashionshop.dto.request.*;
import com.fashionshop.dto.response.AuthResponse;
import com.fashionshop.dto.response.UserSummaryResponse;
import com.fashionshop.entity.PasswordResetToken;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.exception.UnauthorizedException;
import com.fashionshop.repository.PasswordResetTokenRepository;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .provider(User.Provider.LOCAL)
                .build();

        String refreshToken = jwtService.generateRefreshToken();
        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummaryResponse.from(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.isDeleted() || user.getStatus() == User.Status.INACTIVE) {
            throw new UnauthorizedException("Account is inactive or disabled");
        }

        if (user.getProvider() != User.Provider.LOCAL || user.getPassword() == null) {
            throw new BadRequestException("Please login with " + user.getProvider().name().toLowerCase());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String refreshToken = jwtService.generateRefreshToken();
        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummaryResponse.from(user))
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        // Refresh token is just a UUID; we find user by matching the hash
        // We search all users — in a real app you'd store email in the refresh token payload
        // For simplicity, the client must also send email or we embed it as a JWT
        // Here we treat refreshToken as a simple opaque token stored as BCrypt hash
        // Client sends raw token; we verify by finding users where BCrypt matches
        // This is O(n) but acceptable for academic project — use Redis in production
        User user = userRepository.findAll().stream()
                .filter(u -> u.getRefreshToken() != null &&
                             passwordEncoder.matches(request.getRefreshToken(), u.getRefreshToken()))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (user.isDeleted() || user.getStatus() == User.Status.INACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }

        // Rotate refresh token
        String newRefreshToken = jwtService.generateRefreshToken();
        user.setRefreshToken(passwordEncoder.encode(newRefreshToken));
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .user(UserSummaryResponse.from(user))
                .build();
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Always return success to prevent email enumeration
        if (user == null || user.getProvider() != User.Provider.LOCAL) {
            return;
        }

        // Delete previous unused tokens
        resetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        resetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public boolean verifyResetToken(String token) {
        return resetTokenRepository.findByTokenAndUsedFalse(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new BadRequestException("Reset token has expired or already been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRefreshToken(null); // Invalidate all sessions
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    /**
     * Called by OAuth2 success handler to create/update user and generate tokens.
     */
    @Transactional
    public AuthResponse handleOAuth2Login(User user) {
        String refreshToken = jwtService.generateRefreshToken();
        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserSummaryResponse.from(user))
                .build();
    }
}
