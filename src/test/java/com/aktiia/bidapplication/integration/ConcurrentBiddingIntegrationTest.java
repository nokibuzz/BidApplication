package com.aktiia.bidapplication.integration;

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
import com.aktiia.bidapplication.service.BidService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrentBiddingIntegrationTest {

    private static final int NUM_BIDDERS = 100;

    @Autowired
    private BidService bidService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    private Auction auction;
    private final List<User> bidders = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        bidders.clear();

        // Create the seller
        final User seller = userRepository.save(
                User.builder()
                        .username("auction_seller")
                        .email("seller@aktiia.com")
                        .password("password")
                        .role(Role.ROLE_ADMIN)
                        .build()
        );

        auction = auctionRepository.save(
                Auction.builder()
                        .title("High-Demand Auction")
                        .description("100 users will bid on this simultaneously")
                        .startingPrice(new BigDecimal("100.00"))
                        .currentHighestBid(new BigDecimal("100.00"))
                        .status(AuctionStatus.OPEN)
                        .seller(seller)
                        .endTime(LocalDateTime.now().plusHours(2))
                        .build()
        );

        // Create 100 bidder users
        for (int i = 1; i <= NUM_BIDDERS; i++) {
            User bidder = userRepository.save(
                    User.builder()
                            .username("bidder_" + i)
                            .email("bidder_" + i + "@aktiia.com")
                            .password("password")
                            .role(Role.ROLE_USER)
                            .build()
            );
            bidders.add(bidder);
        }
    }

    @Test
    @DisplayName("100 concurrent bids: each bid increments by 1, only unique highest bids should be accepted")
    void shouldHandleConcurrentBidsWithPessimisticLocking() throws InterruptedException {
        final CountDownLatch readyLatch = new CountDownLatch(NUM_BIDDERS);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_BIDDERS);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final List<BidResponse> successfulBids = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(NUM_BIDDERS);

        for (int i = 0; i < NUM_BIDDERS; i++) {
            final User bidder = bidders.get(i);
            final BigDecimal bidAmount = new BigDecimal("100.00").add(new BigDecimal(i + 1));

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    final BidRequest request = new BidRequest(bidAmount);
                    BidResponse response = bidService.placeBid(auction.getId(), request, bidder.getUsername());
                    successfulBids.add(response);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected: BidTooLowException for bids that arrive after a higher bid
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await(10, TimeUnit.SECONDS);
        // Start all simultaneously
        startLatch.countDown();
        // Wait for all threads to finish
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue().as("All bidding threads should complete within timeout");

        // Verify results
        int total = successCount.get() + failureCount.get();
        assertThat(total).isEqualTo(NUM_BIDDERS)
                .as("All %d bids should have been processed (success or failure)", NUM_BIDDERS);

        // At least 1 bid should have succeeded
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        // The final highest bid on the auction should reflect reality
        Auction finalAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(finalAuction.getStatus()).isEqualTo(AuctionStatus.OPEN);

        // The currentHighestBid must be one of the bid amounts that succeeded
        assertThat(finalAuction.getCurrentHighestBid()).isGreaterThan(new BigDecimal("100.00"));

        // All persisted bids must have strictly increasing amounts per insertion order
        List<Bid> allBids = bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId());
        assertThat(allBids).hasSameSizeAs(successfulBids);

        // Verify no two accepted bids have the same amount
        List<BigDecimal> amounts = allBids.stream().map(Bid::getAmount).toList();
        assertThat(amounts).doesNotHaveDuplicates();

        // The highest persisted bid must match the auction's currentHighestBid
        if (!allBids.isEmpty()) {
            assertThat(allBids.getFirst().getAmount()).isEqualByComparingTo(finalAuction.getCurrentHighestBid());
        }

        log.info("Total bidders: {}", NUM_BIDDERS);
        log.info("Successful bids: {}", successCount.get());
        log.info("Rejected bids: {}", failureCount.get());
        log.info("Final highest bid: {}", finalAuction.getCurrentHighestBid());
    }

    @Test
    @DisplayName("100 concurrent bids with same amount: only one should win")
    void shouldAcceptOnlyOneBidWhenAllBidSameAmount() throws InterruptedException {
        final CountDownLatch readyLatch = new CountDownLatch(NUM_BIDDERS);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(NUM_BIDDERS);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        final BigDecimal sameAmount = new BigDecimal("200.00");

        final ExecutorService executor = Executors.newFixedThreadPool(NUM_BIDDERS);

        for (int i = 0; i < NUM_BIDDERS; i++) {
            final User bidder = bidders.get(i);

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    BidRequest request = new BidRequest(sameAmount);
                    bidService.placeBid(auction.getId(), request, bidder.getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Only ONE bid should succeed since all bids are for the same amount.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(NUM_BIDDERS - 1);

        Auction finalAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(finalAuction.getCurrentHighestBid()).isEqualByComparingTo(sameAmount);

        List<Bid> allBids = bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId());
        assertThat(allBids).hasSize(1);

        log.info("Total bidders: {}", NUM_BIDDERS);
        log.info("Successful bids: {}", successCount.get());
        log.info("Rejected bids: {}", failureCount.get());
    }
}
