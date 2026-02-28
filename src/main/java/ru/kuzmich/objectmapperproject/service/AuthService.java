package ru.kuzmich.objectmapperproject.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import ru.kuzmich.objectmapperproject.dto.AuthRequestDto;
import ru.kuzmich.objectmapperproject.dto.RefreshTokenRequestDto;

public interface AuthService {

    ResponseEntity<?> login(AuthRequestDto request);

    ResponseEntity<?> refreshToken(RefreshTokenRequestDto request);

    ResponseEntity<?> register(AuthRequestDto request);

    ResponseEntity<?> getCurrentUser(Authentication authentication);
}
