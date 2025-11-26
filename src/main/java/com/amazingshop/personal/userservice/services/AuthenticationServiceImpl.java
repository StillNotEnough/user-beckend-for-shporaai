package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.interfaces.AuthenticationService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public AuthenticationServiceImpl(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public TokenPairResponse performLogin(AuthenticationDTO authenticationDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationDTO.getUsername(),
                authenticationDTO.getPassword()));

        User user = userService.findByUsername(authenticationDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(authenticationDTO.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(authenticationDTO.getUsername());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
        userService.save(user);

        return new TokenPairResponse(
                accessToken,
                jwtUtil.getAccessTokenExpiration(),
                refreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                authenticationDTO.getUsername()
        );
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        String username = jwtUtil.validateTokenAndRetrieveClaim(request.getRefreshToken());

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Удаляем refresh token из БД
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userService.save(user);

        log.info("User logged out successfully: {}", username);
    }
}
