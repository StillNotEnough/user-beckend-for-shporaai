package com.amazingshop.personal.userservice.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    @NotEmpty(message = "Username should be not empty")
    @Size(min = 2, max = 30, message = "Username should be for 2 to 30 symbols")
    private String username;

    @NotEmpty(message = "Password should be not empty")
    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;

    @NotEmpty(message = "Email should be not empty")
    @Email(message = "Email should be valid")
    private String email;
}