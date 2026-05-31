package com.anipals.backend.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "friend_messages",
        indexes = {
                @Index(name = "idx_friend_messages_sender_uid", columnList = "sender_uid"),
                @Index(name = "idx_friend_messages_recipient_uid", columnList = "recipient_uid"),
                @Index(name = "idx_friend_messages_sent_at", columnList = "sent_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FriendMessage {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String senderKey;

    @Column(nullable = false)
    private String senderUid;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false)
    private String recipientKey;

    @Column(nullable = false)
    private String recipientUid;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
