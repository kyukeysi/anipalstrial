package com.anipals.backend.social.dto;

import java.time.LocalDateTime;

public record FriendMessageResponse(
        String id,
        String senderKey,
        String senderUid,
        String senderName,
        String recipientKey,
        String recipientUid,
        String message,
        LocalDateTime sentAt
) {
}
