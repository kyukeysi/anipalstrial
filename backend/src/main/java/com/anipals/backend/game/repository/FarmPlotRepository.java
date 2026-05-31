package com.anipals.backend.game.repository;

import com.anipals.backend.game.entity.FarmPlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FarmPlotRepository extends JpaRepository<FarmPlot, Long> {
    List<FarmPlot> findByPlayerKeyOrderByPlotIndexAsc(String playerKey);

    Optional<FarmPlot> findByPlayerKeyAndPlotIndex(String playerKey, int plotIndex);
}
