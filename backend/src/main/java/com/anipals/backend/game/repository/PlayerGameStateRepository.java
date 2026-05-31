package com.anipals.backend.game.repository;

import com.anipals.backend.game.entity.PlayerGameState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerGameStateRepository extends JpaRepository<PlayerGameState, Long> {
    Optional<PlayerGameState> findByPlayerKey(String playerKey);

    Optional<PlayerGameState> findByUid(String uid);

    Optional<PlayerGameState> findByUidIgnoreCase(String uid);

    Optional<PlayerGameState> findFirstByNameIgnoreCase(String name);

    @Query("""
            select p from PlayerGameState p
            where lower(p.uid) like lower(concat('%', :query, '%'))
               or lower(p.name) like lower(concat('%', :query, '%'))
            order by p.name asc
            """)
    List<PlayerGameState> searchByUidOrName(@Param("query") String query, Pageable pageable);
}
