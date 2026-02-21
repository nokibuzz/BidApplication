package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.auth.JwtAuthenticationEntryPoint;
import com.aktiia.bidapplication.auth.JwtTokenProvider;
import com.aktiia.bidapplication.config.SecurityConfig;
import com.aktiia.bidapplication.model.dto.request.LoginRequest;
import com.aktiia.bidapplication.model.dto.request.RegisterRequest;
import com.aktiia.bidapplication.model.dto.response.AuthResponse;
import com.aktiia.bidapplication.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@aktiia.com");
        validRegisterRequest.setPassword("password");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password");
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        void registerWithValidRequestReturnsCreated() throws Exception {
            final AuthResponse response = AuthResponse.builder()
                    .token("jwt-token")
                    .username("testuser")
                    .build();

            given(authService.register(any(RegisterRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.username").value("testuser"));

            then(authService).should().register(any(RegisterRequest.class));
        }

        @Test
        void registerWithBlankUsernameReturnsBadRequest() throws Exception {
            validRegisterRequest.setUsername("");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registerWithInvalidEmailReturnsBadRequest() throws Exception {
            validRegisterRequest.setEmail("invalid-email");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registerWithShortPasswordReturnsBadRequest() throws Exception {
            validRegisterRequest.setPassword("short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registerWithEmptyBodyReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        void loginWithValidRequestReturnsOk() throws Exception {
            final AuthResponse response = AuthResponse.builder()
                    .token("jwt-token")
                    .username("testuser")
                    .build();

            given(authService.login(any(LoginRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.username").value("testuser"));

            then(authService).should().login(any(LoginRequest.class));
        }

        @Test
        void loginWithBlankUsernameReturnsBadRequest() throws Exception {
            validLoginRequest.setUsername("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void loginWithBlankPasswordReturnsBadRequest() throws Exception {
            validLoginRequest.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void loginWithEmptyBodyReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
