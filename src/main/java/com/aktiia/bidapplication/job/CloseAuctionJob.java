package com.aktiia.bidapplication.job;

import com.aktiia.bidapplication.exception.ResourceNotFoundException;
import com.aktiia.bidapplication.model.entity.Auction;
import com.aktiia.bidapplication.model.enums.AuctionStatus;
import com.aktiia.bidapplication.repository.AuctionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloseAuctionJob implements Job {

    private final AuctionRepository auctionRepository;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        final String auctionIdString = context.getMergedJobDataMap().getString("auctionId");
        final UUID auctionId = UUID.fromString(auctionIdString);

        log.info("Closing auction {}", auctionId);
        final Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));

        if (auction.getStatus() == AuctionStatus.OPEN) {
            auction.setStatus(AuctionStatus.CLOSED);
            auctionRepository.save(auction);
        }
    }
}
