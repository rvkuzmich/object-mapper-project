package ru.kuzmich.objectmapperproject.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.kuzmich.objectmapperproject.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttempt = :failedAttempt WHERE u.username = :username")
    void updateFailedAttempts(@Param("failedAttempt") int failedAttempt, @Param("username") String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountNonLocked = :locked, u.lockTime = :lockTime WHERE u.username = :username")
    void lockUserAccount(@Param("locked") boolean locked, @Param("lockTime") LocalDateTime lockTime, @Param("username") String username);
}
