package com.anipals.backend.social.dto;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        Long id,
        FriendPlayerResponse player,
        LocalDateTime requestedAt
) {
}
