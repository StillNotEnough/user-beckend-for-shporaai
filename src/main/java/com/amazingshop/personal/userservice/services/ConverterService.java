package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.models.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConverterService {

    private final ModelMapper modelMapper;

    @Autowired
    public ConverterService(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public User convertedToPerson(UserDTO userDTO) {
        return modelMapper.map(userDTO, User.class);
    }

    public UserDTO convertedToPersonDTO(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
}