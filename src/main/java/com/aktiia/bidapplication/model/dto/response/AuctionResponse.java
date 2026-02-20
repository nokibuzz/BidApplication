package com.aktiia.bidapplication.model.dto.response;

import com.aktiia.bidapplication.model.enums.AuctionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionResponse(UUID id,
                              String title,
                              String description,
                              BigDecimal startingPrice,
                              BigDecimal currentHighestBid,
                              AuctionStatus status,
                              String sellerUsername,
                              LocalDateTime createdAt,
                              LocalDateTime endTime,
                              int totalBids) {

    @Builder
    public AuctionResponse{}
}
