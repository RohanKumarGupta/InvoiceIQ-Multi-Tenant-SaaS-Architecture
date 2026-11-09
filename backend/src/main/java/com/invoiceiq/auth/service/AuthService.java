package com.invoiceiq.auth.service;

import com.invoiceiq.auth.dto.AuthResponse;
import com.invoiceiq.auth.dto.LoginRequest;
import com.invoiceiq.auth.dto.SignupRequest;
import com.invoiceiq.auth.dto.UserProfileResponse;
import com.invoiceiq.auth.model.RefreshToken;
import com.invoiceiq.auth.model.Role;
import com.invoiceiq.auth.model.User;
import com.invoiceiq.auth.repository.RefreshTokenRepository;
import com.invoiceiq.auth.repository.UserRepository;
import com.invoiceiq.common.exception.ApiException;
import com.invoiceiq.common.exception.ConflictException;
import com.invoiceiq.common.exception.ResourceNotFoundException;
import com.invoiceiq.security.JwtTokenProvider;
import com.invoiceiq.security.UserPrincipal;
import com.invoiceiq.tenant.model.Tenant;
import com.invoiceiq.tenant.model.TenantPlan;
import com.invoiceiq.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    public record AuthResult(AuthResponse response, ResponseCookie refreshCookie) {}

    public AuthResult signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        String slug = generateUniqueSlug(request.tenantName());

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setSlug(slug);
        tenant.setPlan(TenantPlan.FREE);
        tenant = tenantRepository.save(tenant);

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.OWNER)
                .active(true)
                .emailVerified(false)
                .build();
        user.setTenantId(tenant.getId());
        user = userRepository.save(user);

        return generateAuthResult(user);
    }

    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is deactivated");
        }

        return generateAuthResult(user);
    }

    public AuthResult refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        // Rotate: revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        return generateAuthResult(user);
    }

    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserProfileResponse(user);
    }

    // ── Private helpers ──

    private AuthResult generateAuthResult(User user) {
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getRole()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();

        String refreshTokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHash)
                .deviceInfo("web")
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        ResponseCookie cookie = jwtTokenProvider.createRefreshTokenCookie(rawRefreshToken);

        UserProfileResponse userProfile = toUserProfileResponse(user);
        AuthResponse authResponse = new AuthResponse(accessToken, ACCESS_TOKEN_EXPIRY_SECONDS, userProfile);

        return new AuthResult(authResponse, cookie);
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private String generateUniqueSlug(String tenantName) {
        String base = tenantName.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9\\-]", "");

        if (base.isEmpty()) {
            base = "tenant";
        }

        String slug = base;
        if (tenantRepository.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }

    String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
