package com.anipals.backend.social.controller;

import com.anipals.backend.social.dto.*;
import com.anipals.backend.social.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    public FriendSummaryResponse summary(@RequestParam(required = false) String playerKey) {
        return socialService.summary(playerKey);
    }

    @GetMapping("/search")
    public List<PlayerSearchResponse> search(
            @RequestParam(required = false) String playerKey,
            @RequestParam String query
    ) {
        return socialService.searchPlayers(playerKey, query);
    }

    @PostMapping("/requests")
    public FriendRequestResponse sendRequest(@RequestBody FriendActionRequest request) {
        return socialService.sendFriendRequest(request);
    }

    @PostMapping("/requests/{requestId}/accept")
    public FriendPlayerResponse acceptRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String playerKey
    ) {
        return socialService.acceptRequest(requestId, playerKey);
    }

    @PostMapping("/requests/{requestId}/decline")
    public ResponseEntity<Void> declineRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String playerKey
    ) {
        socialService.declineOrCancelRequest(requestId, playerKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String playerKey
    ) {
        socialService.declineOrCancelRequest(requestId, playerKey);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable String uid,
            @RequestParam(required = false) String playerKey
    ) {
        socialService.removeFriend(playerKey, uid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uid}/block")
    public FriendPlayerResponse blockPlayer(
            @PathVariable String uid,
            @RequestParam(required = false) String playerKey
    ) {
        return socialService.blockPlayer(playerKey, uid);
    }

    @GetMapping("/{uid}/farm")
    public FriendFarmPreviewResponse previewFarm(@PathVariable String uid) {
        return socialService.previewFarm(uid);
    }

    @GetMapping("/{uid}/messages")
    public List<FriendMessageResponse> messages(
            @PathVariable String uid,
            @RequestParam(required = false) String playerKey
    ) {
        return socialService.messagesFor(uid, playerKey);
    }

    @PostMapping("/messages")
    public FriendMessageResponse sendMessage(@RequestBody FriendMessageRequest request) {
        return socialService.sendMessage(request);
    }
}
