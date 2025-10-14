package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UsersRepository usersRepository;

    @Autowired
    public UserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public Optional<User> findPersonByPersonName(String username) {
        log.debug("Searching for person by username: {}", username);
        return usersRepository.findPersonByUsername(username);
    }

    public Optional<User> findPersonByEmail(String email) {
        log.debug("Searching for person by email: {}", email);
        return usersRepository.findPersonByEmail(email);
    }

    public User findPersonByIdOrThrow(Long id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Person with id " + id + " not found"));
    }

    public List<User> findAll() {
        return usersRepository.findAll();
    }

    public boolean existsByUsername(String username) {
        return usersRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return usersRepository.existsByEmail(email);
    }

    @Transactional
    public User save(User user) {
        log.debug("Saving person: {}", user.getUsername());
        return usersRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting person with id: {}", id);
        if (!usersRepository.existsById(id)) {
            throw new UserNotFoundException("Person with id " + id + " not found");
        }
        usersRepository.deleteById(id);
    }
}