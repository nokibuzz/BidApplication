package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.auth.JwtAuthenticationEntryPoint;
import com.aktiia.bidapplication.auth.JwtTokenProvider;
import com.aktiia.bidapplication.config.SecurityConfig;
import com.aktiia.bidapplication.model.dto.request.AuctionRequest;
import com.aktiia.bidapplication.model.dto.response.AuctionResponse;
import com.aktiia.bidapplication.service.AuctionService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
@Import(SecurityConfig.class)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private ObjectMapper objectMapper;

    private AuctionRequest validRequest;
    private final UUID auctionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        validRequest = new AuctionRequest(
                "Test Auction",
                "Very nice item for bidding",
                new BigDecimal("150.00"),
                60L
        );
    }

    @Nested
    @DisplayName("Authorization Tests for Create Auction")
    class AuthorizationTests {

        @Test
        @WithMockUser(roles = "USER")
        void createAuctionAsUserReturnsForbidden() throws Exception {
            mockMvc.perform(post("/api/auctions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createAuctionAsAdminReturnsCreated() throws Exception {
            given(auctionService.createAuction(any(), any())).willReturn(AuctionResponse.builder().build());

            mockMvc.perform(post("/api/auctions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Validation Tests for Auctions")
    class ValidationTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void createAuctionWithTooLongTitleReturnsBadRequest() throws Exception {
            validRequest.setTitle("A".repeat(101)); // Exceeds @Size(max = 100)

            mockMvc.perform(post("/api/auctions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createAuctionWithShortDurationReturnsBadRequest() throws Exception {
            validRequest.setDurationMinutes(0L);

            mockMvc.perform(post("/api/auctions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void getAuctionReturnsOk() throws Exception {
            given(auctionService.getAuction(auctionId)).willReturn(AuctionResponse.builder().build());

            mockMvc.perform(get("/api/auctions/{id}", auctionId))
                    .andExpect(status().isOk());
        }

        @Test
        void getAllAuctionsPublicAccessReturnsOk() throws Exception {
            given(auctionService.getAllAuctions()).willReturn(List.of());

            mockMvc.perform(get("/api/auctions"))
                    .andExpect(status().isOk());
        }
    }
}
