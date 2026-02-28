package ru.kuzmich.objectmapperproject.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.jwt.LoginAttemptService;
import ru.kuzmich.objectmapperproject.model.Role;
import ru.kuzmich.objectmapperproject.model.User;
import ru.kuzmich.objectmapperproject.repository.UserRepository;
import ru.kuzmich.objectmapperproject.service.AdminService;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final LoginAttemptService loginAttemptService;

    private final UserRepository userRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public Map<String, String> updateUserRole(String username, String role) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(Role.valueOf(role));
        userRepository.save(user);
        logger.info("Role updated for user: {} to {}", username, role);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Role updated successfully");
        response.put("username", username);
        response.put("role", role);
        return response;
    }

    @Override
    public Map<String, String> unlockUser(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            loginAttemptService.unlockUserAccount(username);
            logger.info("Account unlocked by admin: {}", username);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Account unlocked successfully");
            response.put("username", username);
            return  response;
        }
        throw new ResourceNotFoundException("User not found");
    }

    @Override
    public Map<String, String> deleteUser(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()) {
            userRepository.delete(user.get());
            logger.info("User deleted by admin: {}", username);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("username", username);
            return response;
        }
        throw new ResourceNotFoundException("User not found");
    }
}
