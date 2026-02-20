package com.aktiia.bidapplication.service;

import com.aktiia.bidapplication.exception.AuctionClosedException;
import com.aktiia.bidapplication.exception.BadRequestException;
import com.aktiia.bidapplication.exception.ResourceNotFoundException;
import com.aktiia.bidapplication.model.dto.request.AuctionRequest;
import com.aktiia.bidapplication.model.dto.response.AuctionResponse;
import com.aktiia.bidapplication.model.dto.response.AuctionStatusResponse;
import com.aktiia.bidapplication.model.entity.Auction;
import com.aktiia.bidapplication.model.entity.Bid;
import com.aktiia.bidapplication.model.entity.User;
import com.aktiia.bidapplication.model.enums.AuctionStatus;
import com.aktiia.bidapplication.model.enums.Role;
import com.aktiia.bidapplication.repository.AuctionRepository;
import com.aktiia.bidapplication.repository.BidRepository;
import com.aktiia.bidapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private AuctionService auctionService;

    private User seller;
    private Auction auction;
    private AuctionRequest auctionRequest;

    private final UUID sellerId = UUID.randomUUID();
    private final UUID auctionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(sellerId)
                .username("seller1")
                .email("seller@test.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();

        auction = Auction.builder()
                .id(auctionId)
                .title("Test Auction")
                .description("A test auction")
                .startingPrice(new BigDecimal("100.00"))
                .currentHighestBid(new BigDecimal("100.00"))
                .status(AuctionStatus.OPEN)
                .seller(seller)
                .createdAt(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .bids(new ArrayList<>())
                .version(0L)
                .build();

        auctionRequest = new AuctionRequest();
        auctionRequest.setTitle("Test Auction");
        auctionRequest.setDescription("A test auction");
        auctionRequest.setStartingPrice(new BigDecimal("100.00"));
        auctionRequest.setDurationMinutes(60L);
    }

    @Nested
    @DisplayName("createAuction()")
    class CreateAuctionTests {

        @Test
        @DisplayName("Should create auction successfully")
        void shouldCreateAuction() {
            when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
            when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

            final AuctionResponse response = auctionService.createAuction(auctionRequest, "seller1");

            assertThat(response).isNotNull();
            assertThat(response.title()).isEqualTo("Test Auction");
            assertThat(response.startingPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.status()).isEqualTo(AuctionStatus.OPEN);
            assertThat(response.sellerUsername()).isEqualTo("seller1");

            verify(auctionRepository).save(any(Auction.class));
        }

        @Test
        @DisplayName("Should throw when seller not found")
        void shouldThrowWhenSellerNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auctionService.createAuction(auctionRequest, "unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateAuction()")
    class UpdateAuctionTests {

        @Test
        @DisplayName("Should update auction successfully when no bids exist")
        void shouldUpdateAuctionNoBids() {
            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
            when(bidRepository.countByAuctionId(auctionId)).thenReturn(0);
            when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

            final AuctionRequest updateRequest = new AuctionRequest();
            updateRequest.setTitle("Updated Title");
            updateRequest.setDescription("Updated description");
            updateRequest.setStartingPrice(new BigDecimal("200.00"));
            updateRequest.setDurationMinutes(120L);

            final AuctionResponse response = auctionService.updateAuction(auctionId, updateRequest, "seller1");

            assertThat(response).isNotNull();
            verify(auctionRepository).save(any(Auction.class));
        }

        @Test
        @DisplayName("Should throw when non-owner tries to update")
        void shouldThrowWhenNonOwnerUpdates() {
            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

            assertThatThrownBy(() -> auctionService.updateAuction(auctionId, auctionRequest, "otherUser"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("only modify your own");
        }

        @Test
        @DisplayName("Should throw when updating a closed auction")
        void shouldThrowWhenUpdatingClosedAuction() {
            auction.setStatus(AuctionStatus.CLOSED);
            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

            assertThatThrownBy(() -> auctionService.updateAuction(auctionId, auctionRequest, "seller1"))
                    .isInstanceOf(AuctionClosedException.class);
        }

        @Test
        @DisplayName("Should throw when auction not found")
        void shouldThrowWhenAuctionNotFound() {
            final UUID randomAuctionUUID = UUID.randomUUID();
            when(auctionRepository.findById(randomAuctionUUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auctionService.updateAuction(randomAuctionUUID, auctionRequest, "seller1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAuction() / getAllAuctions() / getOpenAuctions()")
    class QueryTests {

        @Test
        @DisplayName("Should get auction by ID")
        void shouldGetAuctionById() {
            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

            final AuctionResponse response = auctionService.getAuction(auctionId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(auctionId);
        }

        @Test
        @DisplayName("Should return all auctions")
        void shouldReturnAllAuctions() {
            when(auctionRepository.findAll()).thenReturn(List.of(auction));

            final List<AuctionResponse> result = auctionService.getAllAuctions();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return only open auctions")
        void shouldReturnOpenAuctions() {
            when(auctionRepository.findByStatus(AuctionStatus.OPEN)).thenReturn(List.of(auction));

            final List<AuctionResponse> result = auctionService.getOpenAuctions();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo(AuctionStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("getAuctionStatus()")
    class StatusTests {

        @Test
        @DisplayName("Should return auction status with highest bidder")
        void shouldReturnStatusWithBids() {
            final User bidder = User.builder().id(UUID.randomUUID()).username("bidder1").build();
            final Bid topBid = Bid.builder()
                    .id(UUID.randomUUID())
                    .amount(new BigDecimal("150.00"))
                    .bidder(bidder)
                    .auction(auction)
                    .build();

            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
            when(bidRepository.findTopBidsByAuctionId(auctionId, 10)).thenReturn(List.of(topBid));
            when(bidRepository.countByAuctionId(auctionId)).thenReturn(1);

            final AuctionStatusResponse status = auctionService.getAuctionStatus(auctionId);

            assertThat(status.highestBidderUsername()).isEqualTo("bidder1");
            assertThat(status.totalBids()).isEqualTo(1);
            assertThat(status.status()).isEqualTo(AuctionStatus.OPEN);
        }

        @Test
        @DisplayName("Should return status with no bidder when no bids exist")
        void shouldReturnStatusWithNoBids() {
            when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
            when(bidRepository.findTopBidsByAuctionId(auctionId, 10)).thenReturn(Collections.emptyList());
            when(bidRepository.countByAuctionId(auctionId)).thenReturn(0);

            AuctionStatusResponse status = auctionService.getAuctionStatus(auctionId);

            assertThat(status.highestBidderUsername()).isNull();
            assertThat(status.totalBids()).isEqualTo(0);
        }
    }
}
