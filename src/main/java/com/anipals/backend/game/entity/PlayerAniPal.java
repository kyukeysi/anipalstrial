package com.anipals.backend.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_anipals")
@Getter
@Setter
@NoArgsConstructor
public class PlayerAniPal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String mood;

    private int level;
    private String activeBoost;

    @Column(nullable = false)
    private String palette;
}
