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
import com.aktiia.bidapplication.repository.AuctionRepository;
import com.aktiia.bidapplication.repository.BidRepository;
import com.aktiia.bidapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * Places a bid on an auction.
     * <p>
     * Uses PESSIMISTIC_WRITE lock on the auction row to handle concurrent bids.
     * This ensures that when multiple users bid simultaneously, bids are serialized
     * at the database level — preventing race conditions where two users could both
     * read the same "current highest bid" and both succeed.
     * <p>
     * The lock is held for the duration of the transaction and released on commit/rollback.
     */
    @Transactional
    public BidResponse placeBid(final UUID auctionId, final BidRequest request, final String username) {
        final Auction auction = auctionRepository.findByIdWithPessimisticLock(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));

        // Validate auction is still open
        if (auction.getStatus() == AuctionStatus.CLOSED) {
            throw new AuctionClosedException("This auction is closed and no longer accepts bids");
        }

        // Check if auction has expired (close it if needed)
        if (auction.getEndTime().isBefore(LocalDateTime.now())) {
            auction.setStatus(AuctionStatus.CLOSED);
            auctionRepository.save(auction);
            throw new AuctionClosedException("This auction has expired");
        }

        // Resolve bidder
        final User bidder = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Seller cannot bid on their own auction
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new BadRequestException("You cannot bid on your own auction");
        }

        // Validate bid amount is higher than current highest
        if (request.getAmount().compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new BidTooLowException(
                    "Bid must be higher than current highest bid of " + auction.getCurrentHighestBid()
            );
        }

        // Create and persist the bid
        final Bid bid = Bid.builder()
                .amount(request.getAmount())
                .auction(auction)
                .bidder(bidder)
                .build();

        bidRepository.save(bid);

        // Update the auction's current highest bid
        auction.setCurrentHighestBid(request.getAmount());
        auctionRepository.save(auction);

        log.info("Bid placed: auctionId={}, bidder={}, amount={}", auctionId, username, request.getAmount());

        return mapToResponse(bid);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidsForAuction(final UUID auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new ResourceNotFoundException("Auction", "id", auctionId);
        }

        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private BidResponse mapToResponse(final Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .amount(bid.getAmount())
                .bidderUsername(bid.getBidder().getUsername())
                .auctionId(bid.getAuction().getId())
                .placedAt(bid.getPlacedAt())
                .build();
    }
}
