package com.amazingshop.personal.userservice.controllers;


import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.OAuth2LoginRequest;
import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.OAuth2UserInfo;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import com.amazingshop.personal.userservice.services.ConverterService;
import com.amazingshop.personal.userservice.services.OAuth2Service;
import com.amazingshop.personal.userservice.services.RegistrationService;
import com.amazingshop.personal.userservice.dto.responses.JwtResponse;
import com.amazingshop.personal.userservice.util.validators.UserValidator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final UserValidator userValidator;
    private final RegistrationService registrationService;
    private final JwtUtil jwtUtil;
    private final ConverterService converterService;
    private final AuthenticationManager authenticationManager;
    private final OAuth2Service oAuth2Service;

    @Autowired
    public AuthController(UserValidator userValidator, RegistrationService registrationService,
                          JwtUtil jwtUtil, ConverterService converterService, AuthenticationManager authenticationManager,
                          OAuth2Service oAuth2Service) {
        this.userValidator = userValidator;
        this.registrationService = registrationService;
        this.jwtUtil = jwtUtil;
        this.converterService = converterService;
        this.authenticationManager = authenticationManager;
        this.oAuth2Service = oAuth2Service;
    }

    /**
     * Регистрация нового пользователя
     * POST /api/v1/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<JwtResponse> performRegistration(@RequestBody @Valid UserDTO userDTO) {
        log.info("Registration attempt for username: {}", userDTO.getUsername());

        User user = converterService.convertedToPerson(userDTO);
        userValidator.validateAndThrow(user);
        registrationService.register(user);

        String token = jwtUtil.generateToken(user.getUsername());
        long expiresIn = jwtUtil.getExpirationTime();

        log.info("User registered successfully: {}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new JwtResponse(token, expiresIn, user.getUsername()));
    }

    /**
     * Вход в существующий аккаунт
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> performLogin(@RequestBody @Valid AuthenticationDTO authenticationDTO) {
        log.info("Login attempt for username: {}", authenticationDTO.getUsername());

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationDTO.getUsername(),
                authenticationDTO.getPassword()));

        String token = jwtUtil.generateToken(authenticationDTO.getUsername());
        long expiresIn = jwtUtil.getExpirationTime();

        log.info("User logged in successfully: {}", authenticationDTO.getUsername());
        return ResponseEntity.ok(new JwtResponse(token, expiresIn, authenticationDTO.getUsername()));
    }

    /**
     * OAuth2 Login через Google
     * POST /api/v1/auth/oauth2/google
     */
    @PostMapping("/oauth2/google")
    public ResponseEntity<JwtResponse> loginWithGoogle(@RequestBody @Valid OAuth2LoginRequest request) {
        log.info("Google OAuth2 login attempt");

        try {
            // Верифицируем Google ID Token
            OAuth2UserInfo userInfo = oAuth2Service.verifyGoogleToken(request.getIdToken());

            // Находим или создаем пользователя
            User user = oAuth2Service.findOrCreateOAuth2User(userInfo);

            // Генерируем JWT токен
            String token = jwtUtil.generateToken(user.getUsername());
            long expiresIn = jwtUtil.getExpirationTime();

            log.info("Google OAuth2 login successful for: {}", user.getUsername());
            return ResponseEntity.ok(new JwtResponse(token, expiresIn, user.getUsername()));

        } catch (Exception e) {
            log.error("Google OAuth2 login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Проверка работоспособности сервиса
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth service is running");
    }
}