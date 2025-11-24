package com.amazingshop.personal.userservice.dto.responses;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class UserResponse {

    private List<UserDTO> userDTOS;
}