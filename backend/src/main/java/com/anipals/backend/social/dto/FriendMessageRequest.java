package com.anipals.backend.social.dto;

public record FriendMessageRequest(
        String senderKey,
        String recipientUid,
        String message
) {
}
