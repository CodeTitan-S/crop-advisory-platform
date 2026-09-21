package com.college.cropadvisory.service;

import com.college.cropadvisory.config.JwtTokenProvider;
import com.college.cropadvisory.dto.LoginRequest;
import com.college.cropadvisory.dto.SignupRequest;
import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.ConflictException;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Roles a visitor may assign themselves. ADMIN is deliberately absent: admin accounts are
     * created only by AdminSeeder from environment variables, so taking the requested role at face
     * value here would let anyone mint an admin with a crafted signup request.
     */
    private static final Set<Role> SELF_SIGNUP_ROLES = EnumSet.of(Role.FARMER, Role.OFFICER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public User registerUser(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Logged so repeated attempts against one address are visible without an account leak.
            log.warn("Signup rejected: email already registered");
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(resolveSelfSignupRole(request.getRole()));
        User saved = userRepository.save(user);
        log.info("Registered {} account for {}", saved.getRole(), saved.getEmail());
        return saved;
    }

    public String authenticateUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            // Never log the submitted password, and let the caller map this to a 401.
            log.warn("Login failed for {}", request.getEmail());
            throw ex;
        }
        User user = getUserByEmail(request.getEmail());
        log.info("Login succeeded for {} ({})", user.getEmail(), user.getRole());
        return tokenProvider.generateToken(user.getEmail(), user.getRole().name());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** An unknown role is a malformed request (400); a known but privileged one is forbidden (403). */
    private Role resolveSelfSignupRole(String requested) {
        Role role;
        try {
            role = Role.valueOf(requested == null ? "" : requested.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown role: " + requested);
        }
        if (!SELF_SIGNUP_ROLES.contains(role)) {
            throw new ForbiddenException(
                    "Role " + role + " cannot be self-registered; contact an administrator");
        }
        return role;
    }
}
