package com.anipals.backend.game.repository;

import com.anipals.backend.game.entity.PlayerGameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerGameStateRepository extends JpaRepository<PlayerGameState, Long> {
    Optional<PlayerGameState> findByPlayerKey(String playerKey);

    Optional<PlayerGameState> findByUid(String uid);
}
