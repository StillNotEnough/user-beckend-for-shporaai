package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.UserResponse;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.services.AdminService;
import com.amazingshop.personal.userservice.services.ConverterService;
import com.amazingshop.personal.userservice.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UsersController {

    private final AdminService adminService;
    private final UserService userService;
    private final ConverterService converterService;

    @Autowired
    public UsersController(AdminService adminService, UserService userService, ConverterService converterService) {
        this.adminService = adminService;
        this.userService = userService;
        this.converterService = converterService;
    }

    /**
     * Получение информации о текущем пользователе
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userService.findPersonByPersonName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        UserDTO userDTO = converterService.convertedToPersonDTO(user);
        log.info("User info requested: {}", user.getUsername());

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Обновление информации о текущем пользователе
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(@RequestBody UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userService.findPersonByPersonName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Обновляем только разрешенные поля
        currentUser.setEmail(userDTO.getEmail());
        // Не обновляем username, role, password через этот эндпоинт

        User updatedUser = userService.save(currentUser);
        UserDTO updatedDTO = converterService.convertedToPersonDTO(updatedUser);

        log.info("User info updated: {}", updatedUser.getUsername());
        return ResponseEntity.ok(updatedDTO);
    }

    /**
     * Админский эндпоинт - приветствие
     * GET /api/v1/users/admin/hello
     */
    @GetMapping("/admin/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> helloForAdmin() {
        String message = adminService.sayForAdmin();
        log.info("Admin hello endpoint accessed");
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Админский эндпоинт - получение всех пользователей
     * GET /api/v1/users/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserDTO> userDTOS = users.stream()
                .map(converterService::convertedToPersonDTO)
                .toList();

        log.info("All users requested by admin, count: {}", userDTOS.size());
        return ResponseEntity.ok(new UserResponse(userDTOS));
    }

    /**
     * Админский эндпоинт - получение пользователя по ID
     * GET /api/v1/users/admin/{id}
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findPersonByIdOrThrow(id);
        UserDTO userDTO = converterService.convertedToPersonDTO(user);

        log.info("User requested by admin: {}", user.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Проверка работоспособности сервиса
     * GET /api/v1/users/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Users service is running");
    }
}
