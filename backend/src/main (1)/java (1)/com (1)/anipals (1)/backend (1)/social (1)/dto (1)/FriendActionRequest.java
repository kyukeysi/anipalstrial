package com.anipals.backend.social.dto;

public record FriendActionRequest(
        String playerKey,
        String targetUid
) {
}
