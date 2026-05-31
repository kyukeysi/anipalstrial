package com.anipals.backend.social.dto;

import java.time.LocalDateTime;

public record FriendMessageResponse(
        String id,
        String senderKey,
        String recipientUid,
        String message,
        LocalDateTime sentAt
) {
}
