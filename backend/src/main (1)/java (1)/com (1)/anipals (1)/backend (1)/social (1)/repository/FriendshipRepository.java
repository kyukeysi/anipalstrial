package com.anipals.backend.social.repository;

import com.anipals.backend.social.entity.Friendship;
import com.anipals.backend.social.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByPlayerAKeyAndPlayerBKey(String playerAKey, String playerBKey);

    @Query("""
            select f from Friendship f
            where (f.playerAKey = :playerKey or f.playerBKey = :playerKey)
              and f.status = :status
            order by f.updatedAt desc
            """)
    List<Friendship> findForPlayerByStatus(@Param("playerKey") String playerKey, @Param("status") FriendshipStatus status);

    List<Friendship> findByRecipientKeyAndStatusOrderByCreatedAtDesc(String recipientKey, FriendshipStatus status);

    List<Friendship> findByRequesterKeyAndStatusOrderByCreatedAtDesc(String requesterKey, FriendshipStatus status);

    long countByPlayerAKeyAndStatusOrPlayerBKeyAndStatus(
            String playerAKey,
            FriendshipStatus playerAStatus,
            String playerBKey,
            FriendshipStatus playerBStatus
    );
}
