package com.aktiia.bidapplication.model.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponse(UUID id,
                          BigDecimal amount,
                          String bidderUsername,
                          UUID auctionId,
                          LocalDateTime placedAt) {

    @Builder
    public BidResponse{}
}
