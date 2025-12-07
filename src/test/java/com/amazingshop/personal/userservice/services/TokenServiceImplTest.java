package com.amazingshop.personal.userservice.services;


import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private static final String TEST_USERNAME = "testUser";

    @Test
    @DisplayName("refreshToken: должен вернуть новые токены для валидного refresh token")
    void refreshToken_ShouldReturnNewTokens_WhenValid() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(VALID_REFRESH_TOKEN);

        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setRefreshToken(VALID_REFRESH_TOKEN);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));

        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType(VALID_REFRESH_TOKEN)).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(TEST_USERNAME)).thenReturn("new_access_token");
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn("new_refresh_token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800L);
        when(userService.save(any(User.class))).thenReturn(user);

        // Act
        TokenPairResponse response = tokenService.refreshToken(request);

        // Assert
        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertEquals(TEST_USERNAME, response.getUsername());
        assertEquals(900L, response.getAccessTokenExpiresIn());
        assertEquals(604800L, response.getRefreshTokenExpiresIn());

        // Verify interactions
        verify(jwtUtil, times(1)).validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN);
        verify(jwtUtil, times(1)).getTokenType(VALID_REFRESH_TOKEN);
        verify(userService, times(1)).findByUsername(TEST_USERNAME);
        verify(jwtUtil, times(1)).generateAccessToken(TEST_USERNAME);
        verify(jwtUtil, times(1)).generateRefreshToken(TEST_USERNAME);
        verify(userService, times(1)).save(argThat(u ->
                u.getRefreshToken().equals("new_refresh_token") && u.getRefreshTokenExpiry() != null
        ));
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение для истекшего токена")
    void refreshToken_ShouldThrowException_WhenTokenExpired() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired_token");

        when(jwtUtil.validateTokenAndRetrieveClaim("expired_token"))
                .thenThrow(new TokenExpiredException("Token expired", null));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> tokenService.refreshToken(request));

        verify(userService, never()).findByUsername(any());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение для невалидного токена")
    void refreshToken_ShouldThrowException_WhenTokenInvalid() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.token");

        when(jwtUtil.validateTokenAndRetrieveClaim("invalid.token"))
                .thenThrow(new JWTVerificationException("Invalid token"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> tokenService.refreshToken(request));

        verify(userService, never()).findByUsername(any());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение для access токена вместо refresh")
    void refreshToken_ShouldThrowException_WhenAccessTokenProvided() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access_token");

        when(jwtUtil.validateTokenAndRetrieveClaim("access_token"))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType("access_token")).thenReturn("access"); // не refresh

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.refreshToken(request));

        assertTrue(exception.getMessage().contains("token type") ||
                exception.getMessage().contains("refresh"));

        verify(userService, never()).findByUsername(any());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение если пользователь не найден")
    void refreshToken_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(VALID_REFRESH_TOKEN);

        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType(VALID_REFRESH_TOKEN)).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> tokenService.refreshToken(request));

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение если токен не совпадает с БД")
    void refreshToken_ShouldThrowException_WhenTokenMismatch() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token_from_request");

        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setRefreshToken("different_token_in_db");
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));

        when(jwtUtil.validateTokenAndRetrieveClaim("token_from_request"))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType("token_from_request")).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.refreshToken(request));

        assertTrue(exception.getMessage().contains("mismatch") ||
                exception.getMessage().contains("token"));

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен выбросить исключение если токен истек в БД")
    void refreshToken_ShouldThrowException_WhenTokenExpiredInDb() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(VALID_REFRESH_TOKEN);

        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setRefreshToken(VALID_REFRESH_TOKEN);
        user.setRefreshTokenExpiry(LocalDateTime.now().minusDays(1)); // истек вчера

        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType(VALID_REFRESH_TOKEN)).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.refreshToken(request));

        assertTrue(exception.getMessage().contains("expired") ||
                exception.getMessage().contains("DB"));

        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken: должен корректно обновить refresh token в БД")
    void refreshToken_ShouldUpdateRefreshTokenInDb() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(VALID_REFRESH_TOKEN);

        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setRefreshToken(VALID_REFRESH_TOKEN);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));

        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType(VALID_REFRESH_TOKEN)).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(TEST_USERNAME)).thenReturn("new_access");
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn("new_refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800L);
        when(userService.save(any(User.class))).thenReturn(user);

        // Act
        tokenService.refreshToken(request);

        // Assert - проверяем что старый токен заменён на новый
        verify(userService).save(argThat(u -> {
            assertNotEquals(VALID_REFRESH_TOKEN, u.getRefreshToken(),
                    "Старый refresh token должен быть заменён");
            assertEquals("new_refresh", u.getRefreshToken(),
                    "Должен быть сохранён новый refresh token");
            assertNotNull(u.getRefreshTokenExpiry(),
                    "Expiry должен быть установлен");
            assertTrue(u.getRefreshTokenExpiry().isAfter(LocalDateTime.now()),
                    "Expiry должен быть в будущем");
            return true;
        }));
    }

    @Test
    @DisplayName("refreshToken: должен корректно установить expiry время")
    void refreshToken_ShouldSetCorrectExpiryTime() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(VALID_REFRESH_TOKEN);

        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setRefreshToken(VALID_REFRESH_TOKEN);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));

        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_REFRESH_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(jwtUtil.getTokenType(VALID_REFRESH_TOKEN)).thenReturn("refresh");
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(TEST_USERNAME)).thenReturn("new_access");
        when(jwtUtil.generateRefreshToken(TEST_USERNAME)).thenReturn("new_refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800L); // 7 дней
        when(userService.save(any(User.class))).thenReturn(user);

        // Act
        tokenService.refreshToken(request);

        // Assert
        verify(userService).save(argThat(u -> {
            LocalDateTime expectedExpiry = LocalDateTime.now().plusSeconds(604800L);
            LocalDateTime actualExpiry = u.getRefreshTokenExpiry();

            // Проверяем что expiry примерно равен ожидаемому (с точностью до 5 секунд)
            long diffSeconds = Math.abs(
                    java.time.Duration.between(expectedExpiry, actualExpiry).getSeconds()
            );
            assertTrue(diffSeconds < 5,
                    "Expiry должен быть установлен примерно на " + 604800L + " секунд в будущем");
            return true;
        }));
    }
}