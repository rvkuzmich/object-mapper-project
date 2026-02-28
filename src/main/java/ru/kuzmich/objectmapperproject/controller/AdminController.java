package ru.kuzmich.objectmapperproject.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.kuzmich.objectmapperproject.model.User;
import ru.kuzmich.objectmapperproject.service.AdminService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(adminService.getUserByUsername(username));
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<Map<String, String>> updateUserRole(@PathVariable String username,
        @RequestParam String role) {
        return ResponseEntity.ok(adminService.updateUserRole(username, role));
    }

    @PutMapping("/users/{username}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable String username) {
        return ResponseEntity.ok(adminService.unlockUser(username));
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        return ResponseEntity.ok(adminService.deleteUser(username));
    }
}
