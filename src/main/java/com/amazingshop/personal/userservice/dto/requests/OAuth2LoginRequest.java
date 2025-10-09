package com.amazingshop.personal.userservice.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2LoginRequest {

    @NotEmpty(message = "ID token is required")
    private String idToken;

    private String provider; // "google", "github", "apple"
}
