package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.models.User;

public interface RegistrationService {
    TokenPairResponse register(UserDTO userDTO);
}
