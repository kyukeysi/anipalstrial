package com.anipals.backend.social.dto;

import java.util.List;

public record FriendSummaryResponse(
        int maxFriends,
        int friendCount,
        List<FriendPlayerResponse> friends,
        List<FriendRequestResponse> incomingRequests,
        List<FriendRequestResponse> outgoingRequests,
        List<FriendPlayerResponse> blockedPlayers
) {
}
