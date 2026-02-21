package com.aktiia.bidapplication.service;

import com.aktiia.bidapplication.exception.AuctionClosedException;
import com.aktiia.bidapplication.exception.BadRequestException;
import com.aktiia.bidapplication.exception.BidTooLowException;
import com.aktiia.bidapplication.exception.ResourceNotFoundException;
import com.aktiia.bidapplication.model.dto.request.BidRequest;
import com.aktiia.bidapplication.model.dto.response.BidResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BidService bidService;

    private User seller;
    private User bidder;
    private Auction auction;

    private final UUID sellerId = UUID.randomUUID();
    private final UUID bidderId = UUID.randomUUID();
    private final UUID auctionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(sellerId)
                .username("seller1")
                .email("seller@aktiia.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .build();

        bidder = User.builder()
                .id(bidderId)
                .username("bidder1")
                .email("bidder@aktiia.com")
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
    }

    @Nested
    @DisplayName("placeBid()")
    class PlaceBidTests {

        @Test
        @DisplayName("Should place bid successfully when amount is higher than current highest")
        void shouldPlaceBidSuccessfully() {
            final BidRequest request = new BidRequest(new BigDecimal("150.00"));

            final UUID bidId = UUID.randomUUID();
            final Bid savedBid = Bid.builder()
                    .id(bidId)
                    .amount(new BigDecimal("150.00"))
                    .bidder(bidder)
                    .auction(auction)
                    .build();

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
            when(userRepository.findByUsername("bidder1")).thenReturn(Optional.of(bidder));
            when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);
            when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

            final BidResponse response = bidService.placeBid(auctionId, request, "bidder1");

            assertThat(response).isNotNull();
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(response.bidderUsername()).isEqualTo("bidder1");

            verify(bidRepository).save(any(Bid.class));
            verify(auctionRepository).save(any(Auction.class));
        }

        @Test
        @DisplayName("Should throw when auction is CLOSED")
        void shouldThrowWhenAuctionClosed() {
            auction.setStatus(AuctionStatus.CLOSED);
            BidRequest request = new BidRequest(new BigDecimal("150.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "bidder1"))
                    .isInstanceOf(AuctionClosedException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("Should throw when auction has expired (time-based)")
        void shouldThrowWhenAuctionExpired() {
            auction.setEndTime(LocalDateTime.now().minusMinutes(5));
            BidRequest request = new BidRequest(new BigDecimal("150.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "bidder1"))
                    .isInstanceOf(AuctionClosedException.class)
                    .hasMessageContaining("expired");

            // Should also mark it as CLOSED
            assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        }

        @Test
        @DisplayName("Should throw when seller bids on own auction")
        void shouldThrowWhenSellerBids() {
            BidRequest request = new BidRequest(new BigDecimal("150.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
            when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "seller1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("own auction");
        }

        @Test
        @DisplayName("Should throw when bid amount is equal to current highest")
        void shouldThrowWhenBidEqualToCurrent() {
            BidRequest request = new BidRequest(new BigDecimal("100.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
            when(userRepository.findByUsername("bidder1")).thenReturn(Optional.of(bidder));

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "bidder1"))
                    .isInstanceOf(BidTooLowException.class);
        }

        @Test
        @DisplayName("Should throw when bid amount is lower than current highest")
        void shouldThrowWhenBidTooLow() {
            BidRequest request = new BidRequest(new BigDecimal("50.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
            when(userRepository.findByUsername("bidder1")).thenReturn(Optional.of(bidder));

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "bidder1"))
                    .isInstanceOf(BidTooLowException.class)
                    .hasMessageContaining("higher than current highest");
        }

        @Test
        @DisplayName("Should throw when auction not found")
        void shouldThrowWhenAuctionNotFound() {
            BidRequest request = new BidRequest(new BigDecimal("150.00"));

            final UUID randomAuctionId = UUID.randomUUID();
            when(auctionRepository.findByIdWithPessimisticLock(randomAuctionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidService.placeBid(randomAuctionId, request, "bidder1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when bidder not found")
        void shouldThrowWhenBidderNotFound() {
            BidRequest request = new BidRequest(new BigDecimal("150.00"));

            when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bidService.placeBid(auctionId, request, "ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBidsForAuction()")
    class GetBidsTests {

        @Test
        @DisplayName("Should return bids ordered by amount desc")
        void shouldReturnBidsForAuction() {
            final Bid bid1 = Bid.builder().id(UUID.randomUUID()).amount(new BigDecimal("200.00")).bidder(bidder).auction(auction).build();
            final Bid bid2 = Bid.builder().id(UUID.randomUUID()).amount(new BigDecimal("150.00")).bidder(bidder).auction(auction).build();

            when(auctionRepository.existsById(auctionId)).thenReturn(true);
            when(bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)).thenReturn(List.of(bid1, bid2));

            final List<BidResponse> result = bidService.getBidsForAuction(auctionId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should throw when auction does not exist")
        void shouldThrowWhenAuctionNotFound() {
            final UUID randomAuctionId = UUID.randomUUID();
            when(auctionRepository.existsById(randomAuctionId)).thenReturn(false);

            assertThatThrownBy(() -> bidService.getBidsForAuction(randomAuctionId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

