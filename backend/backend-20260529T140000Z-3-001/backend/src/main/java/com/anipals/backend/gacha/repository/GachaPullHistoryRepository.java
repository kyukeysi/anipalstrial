package com.anipals.backend.gacha.repository;

import com.anipals.backend.gacha.entity.GachaPullHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GachaPullHistoryRepository extends JpaRepository<GachaPullHistory, Long> {
    List<GachaPullHistory> findTop20ByPlayerKeyOrderByPulledAtDesc(String playerKey);
}
