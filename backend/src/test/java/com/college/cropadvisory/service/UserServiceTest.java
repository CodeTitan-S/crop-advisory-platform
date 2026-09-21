package com.college.cropadvisory.service;

import com.college.cropadvisory.config.JwtTokenProvider;
import com.college.cropadvisory.dto.LoginRequest;
import com.college.cropadvisory.dto.SignupRequest;
import com.college.cropadvisory.exception.BadRequestException;
import com.college.cropadvisory.exception.ForbiddenException;
import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * Tests registration, authentication, and email lookup without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    private User sampleFarmer;
    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        sampleFarmer = new User();
        sampleFarmer.setId(1L);
        sampleFarmer.setName("John Doe");
        sampleFarmer.setEmail("john@example.com");
        sampleFarmer.setPasswordHash("encodedPassword");
        sampleFarmer.setRole(Role.FARMER);

        signupRequest = new SignupRequest();
        signupRequest.setName("John Doe");
        signupRequest.setEmail("john@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRole("FARMER");
    }

    // ─── registerUser ───────────────────────────────────────────────────

    /** Happy path: new user is saved with encoded password and correct role. */
    @Test
    @DisplayName("registerUser – success: saves user with encoded password")
    void registerUser_success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleFarmer);

        User result = userService.registerUser(signupRequest);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals(Role.FARMER, result.getRole());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    /** Duplicate email should throw RuntimeException. */
    @Test
    @DisplayName("registerUser – fail: duplicate email throws exception")
    void registerUser_duplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerUser(signupRequest));

        assertEquals("Email already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    /** An unrecognised role string is a malformed request. */
    @Test
    @DisplayName("registerUser – fail: invalid role throws exception")
    void registerUser_invalidRole() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        signupRequest.setRole("INVALID_ROLE");

        assertThrows(BadRequestException.class,
                () -> userService.registerUser(signupRequest));
        verify(userRepository, never()).save(any());
    }

    /**
     * Self-registering as ADMIN must be refused: admin accounts come only from AdminSeeder, and
     * accepting the requested role would otherwise let anyone mint an admin over HTTP.
     */
    @Test
    @DisplayName("registerUser – fail: ADMIN cannot be self-registered")
    void registerUser_adminRoleForbidden() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        signupRequest.setRole("ADMIN");

        assertThrows(ForbiddenException.class,
                () -> userService.registerUser(signupRequest));
        verify(userRepository, never()).save(any());
    }

    /** The check is case-insensitive, so a lowercase "admin" cannot slip through either. */
    @Test
    @DisplayName("registerUser – fail: lowercase 'admin' also forbidden")
    void registerUser_adminRoleForbiddenCaseInsensitive() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        signupRequest.setRole("admin");

        assertThrows(ForbiddenException.class,
                () -> userService.registerUser(signupRequest));
        verify(userRepository, never()).save(any());
    }

    // ─── authenticateUser ───────────────────────────────────────────────

    /** Happy path: valid credentials return a JWT token string. */
    @Test
    @DisplayName("authenticateUser – success: returns JWT token")
    void authenticateUser_success() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "password123");
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleFarmer));
        when(tokenProvider.generateToken("john@example.com", "FARMER")).thenReturn("jwt-token-123");

        String token = userService.authenticateUser(loginRequest);

        assertEquals("jwt-token-123", token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken("john@example.com", "FARMER");
    }

    /** Bad credentials should propagate the AuthenticationManager's exception. */
    @Test
    @DisplayName("authenticateUser – fail: bad credentials throws exception")
    void authenticateUser_badCredentials() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> userService.authenticateUser(loginRequest));
    }

    // ─── getUserByEmail ─────────────────────────────────────────────────

    /** Happy path: existing email returns the user. */
    @Test
    @DisplayName("getUserByEmail – success: returns user for known email")
    void getUserByEmail_success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleFarmer));

        User result = userService.getUserByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    /** Unknown email should throw RuntimeException. */
    @Test
    @DisplayName("getUserByEmail – fail: unknown email throws exception")
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserByEmail("unknown@example.com"));

        assertEquals("User not found", ex.getMessage());
    }
}
