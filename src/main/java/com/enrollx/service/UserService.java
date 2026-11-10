package com.enrollx.service;

import com.enrollx.dto.StudentRegistrationDto;
import com.enrollx.entity.User;
import com.enrollx.enums.Role;
import com.enrollx.exception.ResourceNotFoundException;
import com.enrollx.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerStudent(StudentRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
        }
        if (userRepository.existsByStudentId(dto.getStudentId().trim())) {
            throw new IllegalArgumentException("Student ID '" + dto.getStudentId() + "' already exists.");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName().trim());
        user.setStudentId(dto.getStudentId().trim());
        user.setRole(Role.ROLE_STUDENT);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> getAllStudents() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.ROLE_STUDENT);
    }

    @Transactional(readOnly = true)
    public long countTotalStudents() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.ROLE_STUDENT).size();
    }
}
