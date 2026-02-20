package com.aktiia.bidapplication.model.dto.response;

import com.aktiia.bidapplication.model.enums.AuctionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AuctionStatusResponse(UUID auctionId,
                                    String title,
                                    AuctionStatus status,
                                    BigDecimal startingPrice,
                                    BigDecimal currentHighestBid,
                                    String highestBidderUsername,
                                    LocalDateTime endTime,
                                    int totalBids,
                                    List<BidResponse> recentBids) {

    @Builder
    public AuctionStatusResponse{}
}
