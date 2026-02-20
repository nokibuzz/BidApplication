package com.aktiia.bidapplication.repository;

import com.aktiia.bidapplication.model.entity.Auction;
import com.aktiia.bidapplication.model.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithPessimisticLock(@Param("id") UUID id);

    List<Auction> findByStatus(AuctionStatus status);

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);

    List<Auction> findBySellerId(UUID sellerId);
}
