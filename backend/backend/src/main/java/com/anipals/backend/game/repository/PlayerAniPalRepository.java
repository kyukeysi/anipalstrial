package com.anipals.backend.game.repository;

import com.anipals.backend.game.entity.PlayerAniPal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAniPalRepository extends JpaRepository<PlayerAniPal, Long> {
    List<PlayerAniPal> findByPlayerKeyOrderByIdAsc(String playerKey);

    Optional<PlayerAniPal> findByPlayerKeyAndId(String playerKey, Long id);
}
