package com.anipals.backend.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "farm_plots")
@Getter
@Setter
@NoArgsConstructor
public class FarmPlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerKey;

    @Column(nullable = false)
    private int plotIndex;

    @Column(nullable = false)
    private String crop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmPlotState state;

    private LocalDateTime plantedAt;
    private LocalDateTime readyAt;
}
