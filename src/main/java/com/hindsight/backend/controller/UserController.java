package com.hindsight.backend.controller;

import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.User;
import com.hindsight.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("_id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("username", user.getName());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }
}
