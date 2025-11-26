package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.interfaces.TokenService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public TokenServiceImpl(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    @Transactional
    public TokenPairResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Валидация токена и извлечение username
        String username = validateRefreshTokenAndGetUsername(refreshToken);

        // 2. Проверка типа токена
        validateRefreshTokenType(refreshToken);

        // 3. Поиск пользователя
        User user = findUserByUsername(username);

        // 4. Проверка соответствия токена в запросе и токена в БД
        validateRefreshTokenMatch(refreshToken, user);

        // 5. Проверка истечения токена в БД
        validateRefreshTokenExpiry(user);

        // 6. Генерация новых токенов
        String newAccessToken = jwtUtil.generateAccessToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        // 7. Обновление токена в БД
        updateRefreshTokenInDB(user, newRefreshToken);

        log.info("Tokens refreshed successfully for user: {}", username);
        return new TokenPairResponse(
                newAccessToken,
                jwtUtil.getAccessTokenExpiration(),
                newRefreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                username
        );
    }

    private String validateRefreshTokenAndGetUsername(String refreshToken) {
        try {
            String username = jwtUtil.validateTokenAndRetrieveClaim(refreshToken);
            log.debug("Valid refresh token for user: {}", username);
            return username;
        } catch (Exception e) { // Уточни тип исключения, например, TokenExpiredException, SignatureVerificationException
            log.warn("Invalid or expired refresh token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired refresh token", e); // Или кастомное исключение
        }
    }

    private void validateRefreshTokenType(String refreshToken) {
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Invalid token type for refresh: {}", tokenType);
            throw new RuntimeException("Invalid token type for refresh"); // Или кастомное исключение
        }
    }

    private User findUserByUsername(String username) {
        return userService.findByUsername(username) // Используем новое имя метода
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username)); // Или UserNotFoundException
    }

    private void validateRefreshTokenMatch(String refreshTokenFromRequest, User user) {
        String refreshTokenInDB = user.getRefreshToken();
        if (!refreshTokenFromRequest.equals(refreshTokenInDB)) {
            log.warn("Refresh token mismatch for user: {}", user.getUsername());
            throw new RuntimeException("Refresh token mismatch"); // Или кастомное исключение
        }
    }

    private void validateRefreshTokenExpiry(User user) {
        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token expired in DB for user: {}", user.getUsername());
            throw new RuntimeException("Refresh token expired in DB"); // Или кастомное исключение
        }
    }

    private void updateRefreshTokenInDB(User user, String newRefreshToken) {
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
        userService.save(user); // Используем интерфейс
    }
}
