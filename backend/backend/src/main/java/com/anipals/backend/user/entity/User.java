package com.anipals.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String username;

    @Column(unique = true, nullable = false)
    private String uid;

    @Column(unique = true)
    private String playerKey;

    private boolean tutorialCompleted;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.uid == null || this.uid.isBlank()) {
            this.uid = UUID.randomUUID().toString();
        }
        if (this.playerKey == null || this.playerKey.isBlank()) {
            this.playerKey = "player-" + UUID.randomUUID();
        }
    }
}
