package com.aktiia.bidapplication.service;

import com.aktiia.bidapplication.exception.AuctionClosedException;
import com.aktiia.bidapplication.exception.BadRequestException;
import com.aktiia.bidapplication.exception.ResourceNotFoundException;
import com.aktiia.bidapplication.job.CloseAuctionJob;
import com.aktiia.bidapplication.model.dto.request.AuctionRequest;
import com.aktiia.bidapplication.model.dto.response.AuctionResponse;
import com.aktiia.bidapplication.model.dto.response.AuctionStatusResponse;
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
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final Scheduler scheduler;

    @Transactional
    public AuctionResponse createAuction(final AuctionRequest request, final String username) {
        final User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Auction auction = Auction.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .currentHighestBid(request.getStartingPrice())
                .status(AuctionStatus.OPEN)
                .seller(seller)
                .endTime(LocalDateTime.now().plusMinutes(request.getDurationMinutes()))
                .build();

        auction = auctionRepository.save(auction);
        scheduleAuctionCloseJob(auction);

        log.info("Auction created: id={}, title='{}', seller={}, endTime={}",
                auction.getId(), auction.getTitle(), username, auction.getEndTime());

        return mapToResponse(auction);
    }

    @Transactional
    public AuctionResponse updateAuction(final UUID auctionId, final AuctionRequest request, final String username) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));

        // Only the seller can update their own auction
        if (!auction.getSeller().getUsername().equals(username)) {
            throw new BadRequestException("You can only modify your own auctions");
        }

        if (auction.getStatus() == AuctionStatus.CLOSED) {
            throw new AuctionClosedException("Cannot modify a closed auction");
        }

        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());

        // Only allow changing starting price if no bids have been placed
        if (bidRepository.countByAuctionId(auctionId) == 0) {
            auction.setStartingPrice(request.getStartingPrice());
            auction.setCurrentHighestBid(request.getStartingPrice());
        }

        // Allow extending the duration
        if (request.getDurationMinutes() != null) {
            final LocalDateTime newEndTime = auction.getCreatedAt().plusMinutes(request.getDurationMinutes());
            if (newEndTime.isAfter(LocalDateTime.now())) {
                auction.setEndTime(newEndTime);
                rescheduleAuctionCloseJob(auction);
            } else {
                throw new BadRequestException("New end time must be in the future");
            }
        }

        auction = auctionRepository.save(auction);
        log.info("Auction updated: id={}, title='{}'", auction.getId(), auction.getTitle());

        return mapToResponse(auction);
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuction(final UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));
        return mapToResponse(auction);
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getOpenAuctions() {
        return auctionRepository.findByStatus(AuctionStatus.OPEN).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuctionStatusResponse getAuctionStatus(final UUID auctionId) {
        final Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));

        final List<Bid> recentBids = bidRepository.findTopBidsByAuctionId(auctionId, 10);

        // Determine the highest bidder
        final String highestBidderUsername = recentBids.isEmpty()
                ? null
                : recentBids.getFirst().getBidder().getUsername();

        final List<BidResponse> bidResponses = recentBids.stream()
                .map(this::mapBidToResponse)
                .toList();

        return AuctionStatusResponse.builder()
                .auctionId(auction.getId())
                .title(auction.getTitle())
                .status(auction.getStatus())
                .startingPrice(auction.getStartingPrice())
                .currentHighestBid(auction.getCurrentHighestBid())
                .highestBidderUsername(highestBidderUsername)
                .endTime(auction.getEndTime())
                .totalBids(bidRepository.countByAuctionId(auctionId))
                .recentBids(bidResponses)
                .build();
    }

    private void scheduleAuctionCloseJob(final Auction auction) {
        try {

            final JobDetail jobDetail = JobBuilder.newJob(CloseAuctionJob.class)
                    .withIdentity("closeAuctionJob-" + auction.getId())
                    .usingJobData("auctionId", String.valueOf(auction.getId()))
                    .storeDurably()
                    .build();

            final Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("closeAuctionTrigger-" + auction.getId())
                    .startAt(Timestamp.valueOf(auction.getEndTime()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);

        } catch (final SchedulerException e) {
            throw new RuntimeException("Failed to schedule auction close job", e);
        }
    }

    private void deleteAuctionCloseJob(final UUID auctionId) {
        try {
            scheduler.deleteJob(new JobKey("closeAuctionJob-" + auctionId));
        } catch (final SchedulerException e) {
            log.warn("Failed to delete auction close job for auctionId={}", auctionId, e);
        }
    }

    private void rescheduleAuctionCloseJob(final Auction auction) {
        deleteAuctionCloseJob(auction.getId());
        scheduleAuctionCloseJob(auction);
    }

    private AuctionResponse mapToResponse(final Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .description(auction.getDescription())
                .startingPrice(auction.getStartingPrice())
                .currentHighestBid(auction.getCurrentHighestBid())
                .status(auction.getStatus())
                .sellerUsername(auction.getSeller().getUsername())
                .createdAt(auction.getCreatedAt())
                .endTime(auction.getEndTime())
                .totalBids(auction.getBids() != null ? auction.getBids().size() : 0)
                .build();
    }

    private BidResponse mapBidToResponse(final Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .amount(bid.getAmount())
                .bidderUsername(bid.getBidder().getUsername())
                .auctionId(bid.getAuction().getId())
                .placedAt(bid.getPlacedAt())
                .build();
    }
}
