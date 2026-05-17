package com.aditya.Atomburg.api;

import com.aditya.Atomburg.persistence.entity.AppUserEntity;
import com.aditya.Atomburg.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Username is required"));
        }
        
        Optional<AppUserEntity> userOpt = userRepository.findById(username.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }
        
        AppUserEntity user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "role", user.getRole(),
            "department", user.getDepartment() != null ? user.getDepartment() : ""
        ));
    }
}
