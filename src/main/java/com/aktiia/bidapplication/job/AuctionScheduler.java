package com.aktiia.bidapplication.job;

import com.aktiia.bidapplication.model.entity.Auction;
import com.aktiia.bidapplication.model.enums.AuctionStatus;
import com.aktiia.bidapplication.repository.AuctionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;

    @Scheduled(fixedRateString = "${auction.scheduler.fixed-rate-ms}")
    @Transactional
    public void closeExpiredAuctions() {
        final List<Auction> expiredAuctions = auctionRepository.findExpiredAuctions(
                AuctionStatus.OPEN,
                LocalDateTime.now()
        );

        if (expiredAuctions.isEmpty()) {
            return;
        }

        for (Auction auction : expiredAuctions) {
            auction.setStatus(AuctionStatus.CLOSED);
            log.info("Auction closed automatically: id={}, title='{}', highestBid={}",
                    auction.getId(), auction.getTitle(), auction.getCurrentHighestBid());
        }

        auctionRepository.saveAll(expiredAuctions);
        log.info("Closed {} expired auction(s)", expiredAuctions.size());
    }
}
