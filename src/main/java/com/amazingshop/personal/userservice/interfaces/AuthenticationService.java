package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;

public interface AuthenticationService {
    TokenPairResponse performLogin(AuthenticationDTO authenticationDTO);
    void logout(RefreshTokenRequest request);
}
