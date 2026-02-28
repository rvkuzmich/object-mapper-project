package ru.kuzmich.objectmapperproject.jwt;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kuzmich.objectmapperproject.model.User;
import ru.kuzmich.objectmapperproject.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${security.max.login.attempts:5}")
    private int maxAttempts;

    @Value("${security.lock.time.minutes:30}")
    private int lockTimeMinutes;

    private final UserRepository userRepository;

    public void loginSucceeded(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedAttempt() > 0) {
                user.setFailedAttempt(0);
                user.setLockTime(null);
                userRepository.save(user);
                logger.info("Login succeeded for user: {}, failed attempts reset", username);
            }
        });
    }

    public void loginFailed(String username) {
        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            int newFailedAttempts = user.getFailedAttempt() + 1;
            user.setFailedAttempt(newFailedAttempts);

            logger.warn("Login failed for user: {}, attempt {}/{}", username, newFailedAttempts, maxAttempts);

            if (newFailedAttempts >= maxAttempts) {
                lockUserAccount(user);
            } else {
                userRepository.save(user);
            }
        }, () -> {
            logger.warn("Login failed for non-existent user: {}", username);
        });
    }

    private void lockUserAccount(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
        logger.error("Account locked for user: {} due to {} failed attempts", user.getUsername(), user.getFailedAttempt());
    }

    public boolean isAccountLocked(User user) {
        if (!user.isAccountNonLocked()) {
            if (user.getLockTime() == null) {
                return true;
            }

            LocalDateTime unlockTime = user.getLockTime().plusMinutes(lockTimeMinutes);
            if (LocalDateTime.now().isAfter(unlockTime)) {
                user.setAccountNonLocked(true);
                user.setFailedAttempt(0);
                user.setLockTime(null);
                userRepository.save(user);
                logger.info("Account unlocked for user: {} after lock time expired", user.getUsername());
                return false;
            }
            return true;
        }
        return false;
    }

    public void unlockUserAccount(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setAccountNonLocked(true);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            logger.info("Account manually unlocked for user: {}", username);
        });
    }
}
