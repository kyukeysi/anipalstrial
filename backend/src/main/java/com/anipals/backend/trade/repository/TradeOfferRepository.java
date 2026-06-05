package com.anipals.backend.trade.repository;

import com.anipals.backend.trade.entity.TradeOffer;
import com.anipals.backend.trade.entity.TradeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TradeOfferRepository extends JpaRepository<TradeOffer, String> {

    @Query("""
            select distinct t from TradeOffer t
            left join fetch t.items
            where t.initiatorKey = :playerKey or t.recipientKey = :playerKey
            order by t.updatedAt desc
            """)
    List<TradeOffer> findForPlayer(@Param("playerKey") String playerKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TradeOffer t left join fetch t.items where t.id = :id")
    Optional<TradeOffer> findByIdForUpdate(@Param("id") String id);

    long countByInitiatorKeyAndStatus(String initiatorKey, TradeStatus status);
}
