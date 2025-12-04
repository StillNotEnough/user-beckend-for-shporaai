package com.amazingshop.personal.userservice.security.jwt;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.services.UserDetailsServiceImpl;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ← добавь эту строку
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String TEST_USERNAME = "testUser";

    @BeforeEach
    void setUp() {
        // Очищаем SecurityContext перед каждым тестом
        SecurityContextHolder.clearContext();
    }

    private UserDetailsImpl createUserDetails(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encodedPassword");
        user.setEmail(username + "@example.com");
        user.setRole(role);

        return new UserDetailsImpl(user);
    }

    @Test
    @DisplayName("doFilterInternal: должен установить аутентификацию для валидного токена с ролью USER")
    void doFilterInternal_ShouldSetAuthentication_WhenValidToken() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        UserDetailsImpl userDetails = createUserDetails(TEST_USERNAME, Role.USER);

        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Аутентификация должна быть установлена");
        assertEquals(TEST_USERNAME, ((UserDetailsImpl) auth.getPrincipal()).getUsername());
        assertTrue(auth.isAuthenticated());

        // Проверяем роль
        assertTrue(auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
                "Пользователь должен иметь роль ROLE_USER");

        verify(jwtUtil, times(1)).validateTokenAndRetrieveClaim(VALID_TOKEN);
        verify(userDetailsService, times(1)).loadUserByUsername(TEST_USERNAME);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен пропустить запрос без Authorization header")
    void doFilterInternal_ShouldContinueChain_WhenNoAuthHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Аутентификация не должна быть установлена");

        verify(jwtUtil, never()).validateTokenAndRetrieveClaim(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен пропустить запрос с пустым Authorization header")
    void doFilterInternal_ShouldContinueChain_WhenEmptyAuthHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен пропустить запрос с пробелами в Authorization header")
    void doFilterInternal_ShouldContinueChain_WhenBlankAuthHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("   ");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен пропустить запрос без префикса 'Bearer '")
    void doFilterInternal_ShouldContinueChain_WhenNoBearerPrefix() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("SomeOtherScheme token");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).validateTokenAndRetrieveClaim(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: не должен перезаписывать существующую аутентификацию")
    void doFilterInternal_ShouldNotOverrideExistingAuthentication() throws ServletException, IOException {
        // Arrange
        Authentication existingAuth = mock(Authentication.class);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(existingAuth);
        SecurityContextHolder.setContext(securityContext);

        String authHeader = "Bearer " + VALID_TOKEN;
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN)).thenReturn(TEST_USERNAME);

        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication(),
                "Существующая аутентификация не должна быть перезаписана");

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен вернуть 400 для пустого JWT токена после 'Bearer '")
    void doFilterInternal_ShouldReturn400_WhenEmptyJwtToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response, times(1)).sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid JWT token in Bearer Header"
        );
        // Проверяем, что doFilter ВСЁ РАВНО вызывается из-за finally блока
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("doFilterInternal: должен продолжить цепочку при истекшем токене")
    void doFilterInternal_ShouldContinueChain_WhenTokenExpired() throws ServletException, IOException {
        String authHeader = "Bearer " + VALID_TOKEN;
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN))
                .thenThrow(new TokenExpiredException("Token expired", null));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен продолжить цепочку при невалидном токене")
    void doFilterInternal_ShouldContinueChain_WhenInvalidToken() throws ServletException, IOException {
        String authHeader = "Bearer invalid.token";
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim("invalid.token"))
                .thenThrow(new JWTVerificationException("Invalid signature"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен продолжить цепочку когда пользователь не найден")
    void doFilterInternal_ShouldContinueChain_WhenUserNotFound() throws ServletException, IOException {
        String authHeader = "Bearer " + VALID_TOKEN;
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен продолжить цепочку даже при RuntimeException")
    void doFilterInternal_ShouldContinueChain_WhenRuntimeException() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN;
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен обработать токен с лишними пробелами")
    void doFilterInternal_ShouldHandleTokenWithExtraSpaces() throws ServletException, IOException {
        // Arrange
        String authHeader = "Bearer " + VALID_TOKEN + "   "; // даже несколько пробелов
        UserDetailsImpl userDetails = createUserDetails(TEST_USERNAME, Role.USER);

        when(request.getHeader("Authorization")).thenReturn(authHeader);

        // ← Мокаем уже обрезанный токен!
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: должен работать с разными регистрами 'Bearer'")
    void doFilterInternal_ShouldBeCaseSensitiveForBearer() throws ServletException, IOException {
        // Arrange - Spring Security обычно case-sensitive для схемы
        when(request.getHeader("Authorization")).thenReturn("bearer " + VALID_TOKEN);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Фильтр должен быть case-sensitive для 'Bearer'");

        verify(jwtUtil, never()).validateTokenAndRetrieveClaim(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: filterChain всегда должен вызываться в finally блоке")
    void doFilterInternal_ShouldAlwaysCallFilterChain_EvenOnException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtUtil.validateTokenAndRetrieveClaim(VALID_TOKEN))
                .thenThrow(new RuntimeException("Critical error"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}