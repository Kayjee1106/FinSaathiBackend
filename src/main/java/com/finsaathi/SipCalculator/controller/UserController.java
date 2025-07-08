package com.finsaathi.SipCalculator.controller;

import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.repository.UserRepository; // Assuming you have a UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users") // Base path for user-related endpoints
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserRepository userRepository;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        logger.info("Received request to register user: " + user.getName());

        // Basic validation (add more as needed)
        if (user.getName() == null || user.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User name cannot be empty."));
        }

        try {
            // Check if a user with this name already exists (optional, depends on your business logic)
            if (userRepository.findByName(user.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "User with this name already exists."));
            }

            // Save the new user to the database
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully with ID: " + savedUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);

        } catch (Exception e) {
            logger.severe("Error registering user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to register user", "details", e.getMessage()));
        }
    }
}