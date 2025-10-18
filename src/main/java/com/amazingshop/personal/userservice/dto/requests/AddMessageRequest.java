package com.amazingshop.personal.userservice.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {

    @NotEmpty(message = "Content is required")
    private String content;

    @NotEmpty(message = "Role is required")
    private String role;

    private String templateUsed;
}