package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.model.dto.request.BidRequest;
import com.aktiia.bidapplication.model.dto.response.BidResponse;
import com.aktiia.bidapplication.service.BidService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Bid placement and retrieval endpoints")
public class BidController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(@PathVariable final UUID auctionId,
                                                @Valid @RequestBody final BidRequest request,
                                                @AuthenticationPrincipal final UserDetails userDetails) {

        final BidResponse response = bidService.placeBid(auctionId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BidResponse>> getBidsForAuction(@PathVariable final UUID auctionId) {
        final List<BidResponse> response = bidService.getBidsForAuction(auctionId);
        return ResponseEntity.ok(response);
    }
}
