package com.anipals.backend.social.repository;

import com.anipals.backend.social.entity.FriendMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendMessageRepository extends JpaRepository<FriendMessage, String> {

    @Query("""
            select message from FriendMessage message
            where (lower(message.senderUid) = lower(:firstUid) and lower(message.recipientUid) = lower(:secondUid))
               or (lower(message.senderUid) = lower(:secondUid) and lower(message.recipientUid) = lower(:firstUid))
            order by message.sentAt asc
            """)
    List<FriendMessage> findConversation(
            @Param("firstUid") String firstUid,
            @Param("secondUid") String secondUid
    );
}
