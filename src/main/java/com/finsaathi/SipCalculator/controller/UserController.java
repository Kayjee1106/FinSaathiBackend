package com.finsaathi.SipCalculator.controller;

import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users") // Base path for user-related endpoints
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User name cannot be empty."));
        }
        if (email == null || email.isBlank() || !email.contains("@") || !email.contains(".")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid email address is required."));
        }

        User user = userService.createUser(name, email);
        logger.info("User registered/retrieved: " + user.getName() + " with ID: " + user.getId() + ", Email: " + user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User created or retrieved successfully",
                "userId", user.getId().toString(),
                "userName", user.getName(),
                "userEmail", user.getEmail()
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable UUID userId) {
        logger.info("Received request to get user by ID: " + userId);
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(Map.of(
                        "userId", user.getId().toString(),
                        "userName", user.getName(),
                        "userEmail", user.getEmail() != null ? user.getEmail() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
