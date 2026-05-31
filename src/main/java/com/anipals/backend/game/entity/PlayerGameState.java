package com.anipals.backend.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Table(name = "player_game_states")
@Getter
@Setter
@NoArgsConstructor
public class PlayerGameState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String playerKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String farmName;

    @Column(nullable = false, unique = true)
    private String uid;

    private int level;
    private int coins;
    private int gems;
    private int energy;
    private int sprouts;
    private int tickets;
    private int ssrPity;
    private int srPity;
    private boolean guaranteedFeatured;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TutorialState tutorialState;

    private boolean tutorialCompleted;
    private LocalDateTime pondReadyAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        lastSeenAt = now;
        if (uid == null) {
            uid = "ANI-" + (1000 + new Random().nextInt(9000));
        }
    }
}
