package ru.kuzmich.objectmapperproject.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TestJwtUtil {

    private static final String SECRET = "mySuperSecretKeyForJWTTokenGeneration2024With256BitsLength";
    private static final long EXPIRATION = 3600000;

    public static String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "ROLE_" + role);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }

    public static String getUserToken() {
        return generateToken("testuser", "USER");
    }

    public static String getModeratorToken() {
        return generateToken("testmoderator", "MODERATOR");
    }

    public static String getAdminToken() {
        return generateToken("testadmin", "SUPER_ADMIN");
    }
}
