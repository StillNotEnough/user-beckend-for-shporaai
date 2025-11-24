package com.amazingshop.personal.userservice.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=testSecretKeyForTestingOnly1234567890",
        "jwt.expiration=3600"
})
public class JwtUtilTest {

    private final JwtUtil jwtUtil;

    private static final String TEST_USERNAME = "testuser";

    @Autowired
    public JwtUtilTest(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    @Test
    void generate_and_validateToken_shouldWorkCorrectly(){
        // Act: генерация токена
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Assert (проверка)
        assertNotNull(token, "JWT token should not be null");
        assertFalse(token.isBlank(), "JWT token should not be blank");

        // Act: валидация токена
        String extractedUsername = jwtUtil.validateTokenAndRetrieveClaim(token);

        // Assert: проверка, что из токена извлекается правильный username
        assertEquals(TEST_USERNAME, extractedUsername, "Extracted username should match the original");
    }

    @Test
    void validateToken_shouldThrowExceptionForInvalidToken(){
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        assertThrows(com.auth0.jwt.exceptions.JWTVerificationException.class, () -> {
           jwtUtil.validateTokenAndRetrieveClaim(invalidToken);
        }, "Should throw JWTVerificationException for invalid token");
    }
}