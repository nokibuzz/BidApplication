package com.aktiia.bidapplication.repository;

import com.aktiia.bidapplication.model.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    List<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId);

    @Query("SELECT b FROM Bid b " +
            "WHERE b.auction.id = :auctionId " +
            "ORDER BY b.amount DESC LIMIT :limit")
    List<Bid> findTopBidsByAuctionId(@Param("auctionId") UUID auctionId, @Param("limit") int limit);

    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(UUID auctionId);

    int countByAuctionId(UUID auctionId);
}
