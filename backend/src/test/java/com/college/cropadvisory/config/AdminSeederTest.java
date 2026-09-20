package com.college.cropadvisory.config;

import com.college.cropadvisory.model.entity.Role;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdminSeeder}.
 * The seeder runs on every startup, so the tests focus on it staying inert unless it is explicitly
 * configured and needed.
 */
@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seederWith(String email, String password) {
        return new AdminSeeder(userRepository, passwordEncoder, email, password);
    }

    /** No credentials configured: nothing happens and nothing is queried. */
    @Test
    @DisplayName("seedAdmin – skips when no credentials are configured")
    void seedAdmin_notConfigured() {
        assertFalse(seederWith("", "").seedAdmin());
        assertFalse(seederWith("   ", "   ").seedAdmin());

        verifyNoInteractions(userRepository);
    }

    /** Partially configured is still treated as not configured. */
    @Test
    @DisplayName("seedAdmin – skips when only the email is set")
    void seedAdmin_passwordMissing() {
        assertFalse(seederWith("admin@example.com", "").seedAdmin());

        verifyNoInteractions(userRepository);
    }

    /** An admin already exists, so a redeploy must not create a second one. */
    @Test
    @DisplayName("seedAdmin – skips when an admin already exists")
    void seedAdmin_adminExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertFalse(seederWith("admin@example.com", "secret123").seedAdmin());

        verify(userRepository, never()).save(any(User.class));
    }

    /** The configured email is already taken by a non-admin; refuse rather than promote silently. */
    @Test
    @DisplayName("seedAdmin – skips when the email already belongs to another user")
    void seedAdmin_emailTaken() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertFalse(seederWith("taken@example.com", "secret123").seedAdmin());

        verify(userRepository, never()).save(any(User.class));
    }

    /** Happy path: an admin is created with a hashed password and the ADMIN role. */
    @Test
    @DisplayName("seedAdmin – success: creates an ADMIN with a hashed password")
    void seedAdmin_createsAdmin() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(seederWith("admin@example.com", "secret123").seedAdmin());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("admin@example.com", saved.getValue().getEmail());
        assertEquals(Role.ADMIN, saved.getValue().getRole());
        assertEquals("$2a$hashed", saved.getValue().getPasswordHash());
        assertNotEquals("secret123", saved.getValue().getPasswordHash());
    }
}
