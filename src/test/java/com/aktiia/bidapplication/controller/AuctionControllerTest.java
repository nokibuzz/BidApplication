package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.auth.JwtAuthenticationEntryPoint;
import com.aktiia.bidapplication.auth.JwtTokenProvider;
import com.aktiia.bidapplication.config.SecurityConfig;
import com.aktiia.bidapplication.model.dto.request.AuctionRequest;
import com.aktiia.bidapplication.model.dto.response.AuctionResponse;
import com.aktiia.bidapplication.model.dto.response.AuctionStatusResponse;
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

import static com.aktiia.bidapplication.model.enums.AuctionStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    @DisplayName("Create Auction Tests")
    class CreateAuctionTests {

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
            AuctionResponse response = AuctionResponse.builder()
                    .id(auctionId)
                    .title("Test Auction")
                    .build();

            given(auctionService.getAuction(auctionId)).willReturn(response);

            mockMvc.perform(get("/api/auctions/{id}", auctionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test Auction"));

            then(auctionService).should().getAuction(auctionId);
        }
    }

    @Nested
    @DisplayName("Update Auction Tests")
    class UpdateAuctionTests {

        @Test
        @WithMockUser(username = "adminUser", roles = "ADMIN")
        void updateAuctionAsAdminReturnsOk() throws Exception {
            final AuctionResponse response = AuctionResponse.builder()
                    .id(auctionId)
                    .title("Updated Auction")
                    .build();

            given(auctionService.updateAuction(eq(auctionId), any(), eq("adminUser")))
                    .willReturn(response);

            mockMvc.perform(put("/api/auctions/{id}", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Auction"));

            then(auctionService).should()
                    .updateAuction(eq(auctionId), any(), eq("adminUser"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void updateAuctionAsUserReturnsForbidden() throws Exception {
            mockMvc.perform(put("/api/auctions/{id}", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get All Auctions Tests")
    class GetAllAuctionsTests {

        @Test
        void getAllAuctionsReturnsAll() throws Exception {
            given(auctionService.getAllAuctions())
                    .willReturn(List.of(AuctionResponse.builder().build()));

            mockMvc.perform(get("/api/auctions"))
                    .andExpect(status().isOk());

            then(auctionService).should().getAllAuctions();
        }

        @Test
        void getOnlyOpenAuctionsReturnsOpenOnes() throws Exception {
            given(auctionService.getOpenAuctions())
                    .willReturn(List.of(AuctionResponse.builder().build()));

            mockMvc.perform(get("/api/auctions")
                            .param("openOnly", "true"))
                    .andExpect(status().isOk());

            then(auctionService).should().getOpenAuctions();
        }
    }

    @Nested
    @DisplayName("Get Auction Status Tests")
    class GetAuctionStatusTests {

        @Test
        void getAuctionStatusReturnsOk() throws Exception {
            final AuctionStatusResponse statusResponse = AuctionStatusResponse.builder()
                    .auctionId(auctionId)
                    .status(OPEN)
                    .build();

            given(auctionService.getAuctionStatus(auctionId))
                    .willReturn(statusResponse);

            mockMvc.perform(get("/api/auctions/{id}/status", auctionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OPEN"));

            then(auctionService).should().getAuctionStatus(auctionId);
        }
    }
}
