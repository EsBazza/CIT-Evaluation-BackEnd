package com.alonzo.citeval.controller;

import com.alonzo.citeval.model.dto.UserDTO;
import com.alonzo.citeval.model.dto.UserSyncRequestDTO;
import com.alonzo.citeval.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/public/sync-user")
    public UserDTO syncUser(@Valid @RequestBody UserSyncRequestDTO requestDTO) {
        return userService.syncUser(requestDTO);
    }

    @GetMapping("/admin/users")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }
}
