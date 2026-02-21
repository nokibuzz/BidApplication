package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.auth.JwtAuthenticationEntryPoint;
import com.aktiia.bidapplication.auth.JwtTokenProvider;
import com.aktiia.bidapplication.config.SecurityConfig;
import com.aktiia.bidapplication.model.dto.request.BidRequest;
import com.aktiia.bidapplication.model.dto.response.BidResponse;
import com.aktiia.bidapplication.service.BidService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidController.class)
@Import(SecurityConfig.class)
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BidService bidService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID auctionId = UUID.randomUUID();
    private BidRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new BidRequest();
        validRequest.setAmount(new BigDecimal("200.00"));
    }

    @Nested
    @DisplayName("Place Bid Tests")
    class PlaceBidTests {

        @Test
        @WithMockUser(username = "testUser", roles = "USER")
        void placeBidAsUserReturnsCreated() throws Exception {
            final BidResponse response = BidResponse.builder()
                    .amount(new BigDecimal("200.00"))
                    .bidderUsername("testUser")
                    .build();

            given(bidService.placeBid(eq(auctionId), any(BidRequest.class), eq("testUser")))
                    .willReturn(response);

            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(200.00))
                    .andExpect(jsonPath("$.bidderUsername").value("testUser"));

            then(bidService).should()
                    .placeBid(eq(auctionId), any(BidRequest.class), eq("testUser"));
        }

        @Test
        @WithMockUser(username = "adminUser", roles = "ADMIN")
        void placeBidAsAdminReturnsCreated() throws Exception {
            given(bidService.placeBid(eq(auctionId), any(BidRequest.class), eq("adminUser")))
                    .willReturn(BidResponse.builder().build());

            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "GUEST")
        void placeBidWithInvalidRoleReturnsForbidden() throws Exception {
            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        void placeBidWithNullAmountReturnsBadRequest() throws Exception {
            validRequest.setAmount(null);

            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void placeBidWithZeroAmountReturnsBadRequest() throws Exception {
            validRequest.setAmount(new BigDecimal("0.00"));

            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        void placeBidWithNegativeAmountReturnsBadRequest() throws Exception {
            validRequest.setAmount(new BigDecimal("-10.00"));

            mockMvc.perform(post("/api/auctions/{auctionId}/bids", auctionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Bids Tests")
    class GetBidsTests {

        @Test
        void getBidsForAuctionReturnsOk() throws Exception {
            final List<BidResponse> bids = List.of(
                    BidResponse.builder()
                            .amount(new BigDecimal("200.00"))
                            .bidderUsername("user1")
                            .build()
            );

            given(bidService.getBidsForAuction(auctionId))
                    .willReturn(bids);

            mockMvc.perform(get("/api/auctions/{auctionId}/bids", auctionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].amount").value(200.00))
                    .andExpect(jsonPath("$[0].bidderUsername").value("user1"));

            then(bidService).should().getBidsForAuction(auctionId);
        }
    }
}
