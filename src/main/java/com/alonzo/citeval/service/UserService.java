package com.alonzo.citeval.service;

import com.alonzo.citeval.model.dto.UserDTO;
import com.alonzo.citeval.model.dto.UserSyncRequestDTO;
import com.alonzo.citeval.model.entity.User;
import com.alonzo.citeval.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDTO syncUser(UserSyncRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .map(existingUser -> {
                    existingUser.setName(request.name());
                    existingUser.setOauthProvider(request.oauthProvider());
                    existingUser.setOauthSubject(request.oauthSubject());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(request.email());
                    newUser.setName(request.name());
                    newUser.setOauthProvider(request.oauthProvider());
                    newUser.setOauthSubject(request.oauthSubject());
                    newUser.setRole(request.role() != null ? request.role() : "STUDENT");
                    return userRepository.save(newUser);
                });
        return mapToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isEnabled()
        );
    }
}
