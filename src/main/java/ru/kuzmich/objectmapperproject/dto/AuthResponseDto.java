package ru.kuzmich.objectmapperproject.dto;

import lombok.Data;

@Data
public class AuthResponseDto {

    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String username;
    private String role;
    private Long expiresIn;

    public AuthResponseDto(String token, String refreshToken, String username, String role, Long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
