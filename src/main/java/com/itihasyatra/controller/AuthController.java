package com.itihasyatra.controller;

import com.itihasyatra.model.User;
import com.itihasyatra.repository.GuideApplicationRepository;
import com.itihasyatra.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserRepository userRepository;
    private final GuideApplicationRepository guideApplicationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public AuthController(UserRepository userRepository, 
            GuideApplicationRepository guideApplicationRepository,
            BCryptPasswordEncoder passwordEncoder,
            JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.guideApplicationRepository = guideApplicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered."));
        }
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username already taken."));
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole(signupRequest.getRole());
        user.setVerified(false); 
        user.setLanguages(signupRequest.getLanguages());
        user.setExperience(signupRequest.getExperience());
        user.setCreatedAt(LocalDateTime.now());

        // Generate 6-digit OTP
        String generatedOtp = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        user.setOtp(generatedOtp);

        // Send Real Email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("r.praneethardy@gmail.com");
            message.setTo(user.getEmail());
            message.setSubject("ItihasYatra Verification Code");
            message.setText("Welcome! Your OTP code for ItihasYatra is: " + generatedOtp);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email error: " + e.getMessage());
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Signup successful. Check your email for OTP!"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        return userRepository.findByEmail(email)
                .map(user -> {
                    if (otp != null && otp.equals(user.getOtp())) {
                        user.setVerified(true);
                        userRepository.save(user);
                        return ResponseEntity.ok(Map.of("message", "Verified successfully! You can now login."));
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid OTP."));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found.")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        return userRepository.findByEmail(loginRequest.getEmail())
                .map(user -> {
                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials."));
                    }
                    if (!user.getVerified()) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Please verify your email first."));
                    }
                    
                    user.setOtp(null);
                    userRepository.save(user);

                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Login successful.");
                    response.put("email", user.getEmail());
                    response.put("role", user.getRole());
                    response.put("username", user.getUsername());
                    response.put("token", java.util.UUID.randomUUID().toString());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found.")));
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // --- INNER CLASSES FOR REQUEST DATA ---

    static class SignupRequest {
        private String username;
        private String email;
        private String password;
        private User.Role role;
        private String languages;
        private String experience;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public User.Role getRole() { return role; }
        public void setRole(User.Role role) { this.role = role; }
        public String getLanguages() { return languages; }
        public void setLanguages(String languages) { this.languages = languages; }
        public String getExperience() { return experience; }
        public void setExperience(String experience) { this.experience = experience; }
    }

    static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
