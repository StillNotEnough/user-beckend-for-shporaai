package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.responses.CurrentUserResponse;
import com.amazingshop.personal.userservice.interfaces.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;

    @Autowired
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Получение информации о текущем пользователе
     * GET /api/v1/users/me
     * Возвращает актуальные данные пользователя из БД
     * БЕЗ чувствительной информации (пароля, refresh token)
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        CurrentUserResponse response = userService.getCurrentUserResponse();

        log.info("User info requested for: {}", response.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление информации о текущем пользователе
     * PUT /api/v1/users/me
     * <p>
     * Позволяет обновить email и profile picture
     * Username, password, role обновляются через отдельные эндпоинты
     */
    @PutMapping("/me")
    public ResponseEntity<CurrentUserResponse> updateCurrentUser(@RequestBody Map<String, String> updates) {
        CurrentUserResponse response = userService.updateCurrentUserResponse(updates);

        log.info("User info updated for: {}", response.getUsername());
        return ResponseEntity.ok(response);

    }
}