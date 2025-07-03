package com.finsaathi.SipCalculator.service;

import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service // Marks this class as a Spring Service component
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Autowired // Automatically injects an instance of UserRepository
    private UserRepository userRepository;

    @Transactional // Ensures the entire method executes within a single database transaction
    public User createUser(String name, String email) {
        // Attempt to find an existing user by name
        Optional<User> existingUser = userRepository.findByName(name);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // If user exists, check if email needs updating
            if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
                logger.info("Updating email for existing user: " + name + " from " + user.getEmail() + " to " + email);
                user.setEmail(email);
                return userRepository.save(user); // Save to update the email in the database
            }
            logger.info("User already exists: " + name + ". No email update needed.");
            return user; // Return the existing user if no email update is required
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        logger.info("Creating new user: " + name + " with email: " + email);
        return userRepository.save(newUser); // Save the new user to the database
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id); // Uses Spring Data JPA's built-in findById method
    }

    public Optional<User> getUserByName(String name) {
        return userRepository.findByName(name); // Uses the custom query method defined in UserRepository
    }
}