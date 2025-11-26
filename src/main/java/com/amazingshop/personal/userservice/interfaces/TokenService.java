package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;

public interface TokenService {
    TokenPairResponse refreshToken(RefreshTokenRequest request);
}
