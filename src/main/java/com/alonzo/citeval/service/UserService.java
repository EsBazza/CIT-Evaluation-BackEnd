package com.alonzo.citeval.service;

import com.alonzo.citeval.model.dto.UserDTO;
import com.alonzo.citeval.model.dto.UserSyncRequestDTO;
import com.alonzo.citeval.model.entity.User;
import com.alonzo.citeval.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    
    @Value("${app.google.client-id:}")
    private String googleClientId;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDTO syncUser(UserSyncRequestDTO request) {
        // Task 2: Verify Google ID Token instead of trusting JSON
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(request.idToken());
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String subject = payload.getSubject();

            // Role logic: Only trust backend logic, not frontend input
            String role = email.endsWith("@ua.edu.ph") ? 
                (email.contains("faculty") || email.contains("prof") ? "FACULTY" : "STUDENT") : "GUEST";

            User user = userRepository.findByEmail(email)
                    .map(existingUser -> {
                        existingUser.setName(name);
                        existingUser.setOauthSubject(subject);
                        return userRepository.save(existingUser);
                    })
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(name);
                        newUser.setOauthProvider("google");
                        newUser.setOauthSubject(subject);
                        newUser.setRole(role);
                        return userRepository.save(newUser);
                    });
            return mapToDTO(user);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed", e);
        }
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
        return new UserDTO(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isEnabled());
    }
}
