package com.anipals.backend.social.dto;

public record PlayerSearchResponse(
        String uid,
        String name,
        String farm,
        String relationshipStatus
) {
}
