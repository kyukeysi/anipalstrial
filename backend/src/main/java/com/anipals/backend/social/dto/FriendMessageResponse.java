package com.anipals.backend.social.dto;

import java.time.LocalDateTime;

public record FriendMessageResponse(
        String id,
        String senderKey,
        String senderUid,
        String senderName,
        String recipientUid,
        String message,
        LocalDateTime sentAt
) {
}
