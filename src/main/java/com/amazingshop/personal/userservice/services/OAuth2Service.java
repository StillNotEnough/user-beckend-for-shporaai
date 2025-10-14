package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.OAuth2UserInfo;
import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class OAuth2Service {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final UsersRepository usersRepository;
    private final GoogleIdTokenVerifier googleVerifier;

    @Autowired
    public OAuth2Service(UsersRepository usersRepository,
                         @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
        this.usersRepository = usersRepository;

        // Создаем верификатор для Google ID токенов
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Верификация Google ID Token и получение информации о пользователе
     */
    public OAuth2UserInfo verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken token = googleVerifier.verify(idToken);

            if (token == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            return OAuth2UserInfo.builder()
                    .id(payload.getSubject())
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .provider("google")
                    .build();

        } catch (Exception e) {
            log.error("Error verifying Google token: {}", e.getMessage());
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }

    /**
     * Поиск или создание пользователя через OAuth2
     */
    @Transactional
    public User findOrCreateOAuth2User(OAuth2UserInfo userInfo) {
        log.info("Finding or creating OAuth2 user: {} from {}", userInfo.getEmail(), userInfo.getProvider());

        // Ищем пользователя по OAuth ID
        Optional<User> existingPerson = usersRepository
                .findByOauthProviderAndOauthId(userInfo.getProvider(), userInfo.getId());

        if (existingPerson.isPresent()) {
            log.info("Found existing OAuth2 user: {}", existingPerson.get().getUsername());
            return existingPerson.get();
        }

        // Проверяем, существует ли пользователь с таким email (обычная регистрация)
        Optional<User> emailUser = usersRepository.findPersonByEmail(userInfo.getEmail());

        if (emailUser.isPresent()) {
            User user = emailUser.get();

            // Если у пользователя уже есть обычная регистрация, привязываем OAuth
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(userInfo.getProvider());
                user.setOauthId(userInfo.getId());
                user.setProfilePictureUrl(userInfo.getPicture());

                log.info("Linked OAuth2 to existing user: {}", user.getUsername());
                return usersRepository.save(user);
            }

            throw new RuntimeException("User with this email already exists with different OAuth provider");
        }

        // Создаем нового пользователя
        User newUser = new User();
        newUser.setEmail(userInfo.getEmail());
        newUser.setUsername(generateUsernameFromEmail(userInfo.getEmail()));
        newUser.setOauthProvider(userInfo.getProvider());
        newUser.setOauthId(userInfo.getId());
        newUser.setProfilePictureUrl(userInfo.getPicture());
        newUser.setRole(Role.USER);
        newUser.setCreatedAt(java.time.LocalDateTime.now());
        // Password остается null для OAuth2 пользователей

        User savedUser = usersRepository.save(newUser);
        log.info("Created new OAuth2 user: {}", savedUser.getUsername());

        return savedUser;
    }

    /**
     * Генерация уникального username из email
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (usersRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
