package com.aktiia.bidapplication.controller;

import com.aktiia.bidapplication.model.dto.request.AuctionRequest;
import com.aktiia.bidapplication.model.dto.response.AuctionResponse;
import com.aktiia.bidapplication.model.dto.response.AuctionStatusResponse;
import com.aktiia.bidapplication.service.AuctionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Auction management endpoints")
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody final AuctionRequest request,
                                                         @AuthenticationPrincipal final UserDetails userDetails) {
        final AuctionResponse response = auctionService.createAuction(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuctionResponse> updateAuction(@PathVariable final UUID id,
                                                         @Valid @RequestBody final AuctionRequest request,
                                                         @AuthenticationPrincipal final UserDetails userDetails) {
        final AuctionResponse response = auctionService.updateAuction(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable final UUID id) {
        final AuctionResponse response = auctionService.getAuction(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAllAuctions(
            @RequestParam(required = false, defaultValue = "false") final boolean openOnly) {

        final List<AuctionResponse> response = openOnly
                ? auctionService.getOpenAuctions()
                : auctionService.getAllAuctions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<AuctionStatusResponse> getAuctionStatus(@PathVariable final UUID id) {
        final AuctionStatusResponse response = auctionService.getAuctionStatus(id);
        return ResponseEntity.ok(response);
    }
}
