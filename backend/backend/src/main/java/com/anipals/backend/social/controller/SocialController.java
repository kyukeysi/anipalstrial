package com.anipals.backend.social.controller;

import com.anipals.backend.social.dto.FriendFarmPreviewResponse;
import com.anipals.backend.social.dto.FriendMessageRequest;
import com.anipals.backend.social.dto.FriendMessageResponse;
import com.anipals.backend.social.service.SocialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/{uid}/farm")
    public FriendFarmPreviewResponse previewFarm(@PathVariable String uid) {
        return socialService.previewFarm(uid);
    }

    @GetMapping("/{uid}/messages")
    public List<FriendMessageResponse> messages(@PathVariable String uid) {
        return socialService.messagesFor(uid);
    }

    @PostMapping("/messages")
    public FriendMessageResponse sendMessage(@RequestBody FriendMessageRequest request) {
        return socialService.sendMessage(request);
    }
}
