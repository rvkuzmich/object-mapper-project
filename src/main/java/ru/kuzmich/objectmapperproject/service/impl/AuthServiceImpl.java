package ru.kuzmich.objectmapperproject.service.impl;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.kuzmich.objectmapperproject.dto.AuthRequestDto;
import ru.kuzmich.objectmapperproject.dto.AuthResponseDto;
import ru.kuzmich.objectmapperproject.dto.RefreshTokenRequestDto;
import ru.kuzmich.objectmapperproject.exception.ErrorResponse;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.jwt.CustomUserDetailsService;
import ru.kuzmich.objectmapperproject.jwt.JwtService;
import ru.kuzmich.objectmapperproject.jwt.LoginAttemptService;
import ru.kuzmich.objectmapperproject.model.User;
import ru.kuzmich.objectmapperproject.repository.UserRepository;
import ru.kuzmich.objectmapperproject.service.AuthService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;

    private final CustomUserDetailsService customUserDetailsService;

    private final JwtService jwtService;

    private final LoginAttemptService loginAttemptService;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    @Override
    public ResponseEntity<?> login(AuthRequestDto request) {
        try {
            logger.info("Login attempt for user: {}", request.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(),
                    request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (loginAttemptService.isAccountLocked(user)) {
                logger.warn("Login attempt on locked account: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        "Account Locked",
                        "Account is locked. Try again later.",
                        "/api/auth/login"
                    ));
            }

            loginAttemptService.loginSucceeded(request.getUsername());

            String token = jwtService.generateToken(authentication);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            logger.info("User logged in successfully: {}", request.getUsername());

            return ResponseEntity.ok(new AuthResponseDto(
                token,
                refreshToken,
                userDetails.getUsername(),
                user.getRole().name(),
                jwtService.getExpirationTime()
            ));

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(request.getUsername());
            logger.warn("Invalid credentials for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Authentication Failed",
                    "Invalid username or password",
                    "/api/auth/login"
                ));

        } catch (LockedException e) {
            logger.warn("Locked account attempt: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "Account Locked",
                    "Account is locked",
                    "/api/auth/login"
                ));

        } catch (DisabledException e) {
            logger.warn("Disabled account attempt: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    "Account Disabled",
                    "Account is disabled",
                    "/api/auth/login"
                ));

        } catch (Exception e) {
            logger.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "Authentication failed: " + e.getMessage(),
                    "/api/auth/login"
                ));
        }
    }

    @Override
    public ResponseEntity<?> refreshToken(RefreshTokenRequestDto request) {
        try {
            String refreshToken = request.getRefreshToken();

            if (jwtService.validateToken(refreshToken)) {
                String username = jwtService.extractUsername(refreshToken);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                String newToken = jwtService.generateToken(new HashMap<>(), userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                logger.info("Token refreshed for user: {}", username);

                return ResponseEntity.ok(new AuthResponseDto(
                    newToken,
                    newRefreshToken,
                    userDetails.getUsername(),
                    user.getRole().name(),
                    jwtService.getExpirationTime()
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Invalid Token",
                    "Invalid refresh token",
                    "/api/auth/refresh"
                ));

        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Token Refresh Failed",
                    "Token refresh failed: " + e.getMessage(),
                    "/api/auth/refresh"
                ));
        }
    }

    @Override
    public ResponseEntity<?> register(AuthRequestDto request) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Registration Failed",
                        "Username already exists",
                        "/api/auth/register"
                    ));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(ru.kuzmich.objectmapperproject.model.Role.USER);

            userRepository.save(user);

            logger.info("User registered successfully: {}", request.getUsername());

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", request.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Registration Failed",
                    "Registration failed: " + e.getMessage(),
                    "/api/auth/register"
                ));
        }
    }

    @Override
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Not Authenticated",
                        "User is not authenticated",
                        "/api/auth/me"
                    ));
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("accountNonLocked", user.isAccountNonLocked());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Get current user error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Authentication Error",
                    "Failed to get user info: " + e.getMessage(),
                    "/api/auth/me"
                ));
        }
    }
}
