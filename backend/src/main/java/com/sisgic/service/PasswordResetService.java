package com.sisgic.service;

import com.sisgic.entity.PasswordResetToken;
import com.sisgic.entity.User;
import com.sisgic.repository.PasswordResetTokenRepository;
import com.sisgic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_FORGOT_MESSAGE =
        "If an account exists, password reset instructions have been sent.";
    private static final String SUCCESS_RESET_MESSAGE = "Password has been reset successfully.";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired password reset token.";

    private final Map<String, Instant> recentByIdentifier = new ConcurrentHashMap<>();
    private final Map<String, Instant> recentByIp = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.password-reset.token-expiration-minutes:60}")
    private long tokenExpirationMinutes;

    @Value("${app.password-reset.min-request-interval-seconds:60}")
    private long minRequestIntervalSeconds;

    @Value("${app.password-reset.ip-min-interval-seconds:10}")
    private long ipMinIntervalSeconds;

    @Value("${app.password-reset.write-link-file:false}")
    private boolean writeLinkFile;

    @Value("${app.password-reset.min-password-length:8}")
    private int minPasswordLength;

    @Transactional
    public String forgotPassword(String identifier, String clientIp, String userAgent) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        if (normalizedIdentifier.isEmpty()) {
            return GENERIC_FORGOT_MESSAGE;
        }

        Instant now = Instant.now();
        if (isRateLimited("id:" + normalizedIdentifier.toLowerCase(), now, minRequestIntervalSeconds)
            || isRateLimited("ip:" + (clientIp == null ? "unknown" : clientIp), now, ipMinIntervalSeconds)) {
            log.info("Password reset request rate-limited");
            return GENERIC_FORGOT_MESSAGE;
        }

        Optional<User> userOpt = findUserByIdentifier(normalizedIdentifier);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown identifier");
            return GENERIC_FORGOT_MESSAGE;
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Password reset skipped: user has no email configured");
            return GENERIC_FORGOT_MESSAGE;
        }

        tokenRepository.invalidateUnusedTokensForUser(user.getId(), now);

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusSeconds(tokenExpirationMinutes * 60));
        token.setCreatedIp(truncate(clientIp, 64));
        token.setUserAgent(truncate(userAgent, 512));
        tokenRepository.save(token);

        String resetLink = buildResetLink(rawToken);
        try {
            emailService.sendPlainText(
                user.getEmail(),
                "Reset your password - Scientific Products Platform",
                buildEmailBody(resetLink)
            );
        } catch (Exception e) {
            log.error("Password reset email could not be sent");
        }

        maybeWriteResetLinkFile(resetLink);
        return GENERIC_FORGOT_MESSAGE;
    }

    @Transactional
    public String resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
        }
        validatePasswordPolicy(newPassword);

        Instant now = Instant.now();
        String tokenHash = hashToken(rawToken.trim());
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN_MESSAGE));

        if (!token.isValid(now)) {
            throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
        }

        User user = userRepository.findById(token.getUserId())
            .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN_MESSAGE));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(now);
        tokenRepository.save(token);
        tokenRepository.invalidateUnusedTokensForUser(user.getId(), now);

        log.info("Password reset completed for user id={}", user.getId());
        return SUCCESS_RESET_MESSAGE;
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(identifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        return userRepository.findByEmailIgnoreCase(identifier);
    }

    private boolean isRateLimited(String key, Instant now, long intervalSeconds) {
        Instant previous = recentByIdentifier.get(key);
        if (key.startsWith("ip:")) {
            previous = recentByIp.get(key);
        }
        if (previous != null && previous.plusSeconds(intervalSeconds).isAfter(now)) {
            return true;
        }
        if (key.startsWith("ip:")) {
            recentByIp.put(key, now);
        } else {
            recentByIdentifier.put(key, now);
        }
        return false;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash reset token", e);
        }
    }

    private String buildResetLink(String rawToken) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + rawToken;
    }

    private String buildEmailBody(String resetLink) {
        return """
            Hello,

            We received a request to reset your password for the Scientific Products Platform.

            Click the link below to set a new password:

            %s

            This link will expire in %d minutes.

            If you did not request this change, you can safely ignore this email.

            Scientific Products Platform
            """.formatted(resetLink, tokenExpirationMinutes);
    }

    private void validatePasswordPolicy(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (newPassword.length() < minPasswordLength) {
            throw new IllegalArgumentException(
                "Password must be at least " + minPasswordLength + " characters."
            );
        }
    }

    private void maybeWriteResetLinkFile(String resetLink) {
        if (!writeLinkFile) {
            return;
        }
        try {
            Path dir = Path.of("tmp");
            Files.createDirectories(dir);
            Path file = dir.resolve("last-password-reset-link.txt");
            Files.writeString(file, resetLink + System.lineSeparator());
            log.info("Password reset link written to local file for development testing");
        } catch (Exception e) {
            log.warn("Could not write local password reset link file: {}", e.getMessage());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public String getGenericForgotMessage() {
        return GENERIC_FORGOT_MESSAGE;
    }

    public String getInvalidTokenMessage() {
        return INVALID_TOKEN_MESSAGE;
    }
}
