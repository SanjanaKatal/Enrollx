package com.enrollx.repository;

import com.enrollx.entity.User;
import com.enrollx.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentId(String studentId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
