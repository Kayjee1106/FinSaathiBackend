package com.finsaathi.SipCalculator.service;

import com.finsaathi.SipCalculator.model.User;
import com.finsaathi.SipCalculator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUser(String name) { // MODIFIED: Removed email parameter
        Optional<User> existingUser = userRepository.findByName(name);
        if (existingUser.isPresent()) {
            logger.info("User already exists: " + name + ". Returning existing user.");
            return existingUser.get();
        }
        User newUser = new User();
        newUser.setName(name);
        // newUser.setEmail(null); // Email is not set at creation
        logger.info("Creating new user: " + name);
        return userRepository.save(newUser);
    }

    /**
     * NEW METHOD: Updates the email address for an existing user.
     * @param userId The ID of the user to update.
     * @param newEmail The new email address to set.
     * @return An Optional containing the updated User entity if found, or empty if user not found.
     */
    @Transactional
    public Optional<User> updateUserEmail(UUID userId, String newEmail) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
                user.setEmail(newEmail);
                logger.info("Updating email for user " + userId + " to: " + newEmail);
                return Optional.of(userRepository.save(user));
            }
            logger.info("Email for user " + userId + " is already " + user.getEmail() + " or new email is invalid/blank. No update performed.");
            return userOptional; // Return existing user if no update needed
        }
        logger.warning("Attempted to update email for non-existent user: " + userId);
        return Optional.empty();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByName(String name) {
        return userRepository.findByName(name);
    }
}