package com.anipals.backend.gacha.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "gacha_pull_history")
@Getter
@Setter
@NoArgsConstructor
public class GachaPullHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerKey;

    @Column(nullable = false)
    private String result;

    @Column(nullable = false)
    private String rarity;

    private boolean featured;

    @Column(nullable = false)
    private LocalDateTime pulledAt;

    @PrePersist
    public void onCreate() {
        if (pulledAt == null) {
            pulledAt = LocalDateTime.now();
        }
    }
}
