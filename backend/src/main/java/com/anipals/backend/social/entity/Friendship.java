package com.anipals.backend.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "friendships",
        uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"player_a_key", "player_b_key"}),
        indexes = {
                @Index(name = "idx_friendship_requester", columnList = "requester_key"),
                @Index(name = "idx_friendship_recipient", columnList = "recipient_key"),
                @Index(name = "idx_friendship_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_a_key", nullable = false)
    private String playerAKey;

    @Column(name = "player_b_key", nullable = false)
    private String playerBKey;

    @Column(name = "requester_key", nullable = false)
    private String requesterKey;

    @Column(name = "recipient_key", nullable = false)
    private String recipientKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
