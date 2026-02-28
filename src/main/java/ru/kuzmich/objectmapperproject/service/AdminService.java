package ru.kuzmich.objectmapperproject.service;

import java.util.List;
import java.util.Map;
import ru.kuzmich.objectmapperproject.model.User;

public interface AdminService {

    List<User> getAllUsers();

    User getUserByUsername(String username);

    Map<String, String> updateUserRole(String username, String role);

    Map<String, String> unlockUser(String username);

    Map<String, String> deleteUser(String username);
}
