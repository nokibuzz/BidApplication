package com.aktiia.bidapplication.service;

import com.aktiia.bidapplication.auth.JwtTokenProvider;
import com.aktiia.bidapplication.exception.BadRequestException;
import com.aktiia.bidapplication.model.dto.request.LoginRequest;
import com.aktiia.bidapplication.model.dto.request.RegisterRequest;
import com.aktiia.bidapplication.model.dto.response.AuthResponse;
import com.aktiia.bidapplication.model.entity.User;
import com.aktiia.bidapplication.model.enums.Role;
import com.aktiia.bidapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@aktiia.com", "password123");
        loginRequest = new LoginRequest("testuser", "password123");

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@aktiia.com")
                .password("encodedPassword")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user successfully")
        void shouldRegisterNewUser() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@aktiia.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generateToken("testuser")).thenReturn("jwt-token");

            final AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.email()).isEqualTo("test@aktiia.com");

            final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            final User captured = userCaptor.getValue();
            assertThat(captured.getUsername()).isEqualTo("testuser");
            assertThat(captured.getPassword()).isEqualTo("encodedPassword");
            assertThat(captured.getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("Should throw exception when username is taken")
        void shouldThrowWhenUsernameExists() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when email is taken")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@aktiia.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should throw when credentials are invalid")
        void shouldThrowOnInvalidCredentials() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
