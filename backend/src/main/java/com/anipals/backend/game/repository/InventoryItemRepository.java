package com.anipals.backend.game.repository;

import com.anipals.backend.game.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByPlayerKeyOrderByIdAsc(String playerKey);

    Optional<InventoryItem> findByPlayerKeyAndItemCode(String playerKey, String itemCode);

    Optional<InventoryItem> findByPlayerKeyAndId(String playerKey, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByPlayerKeyAndId(String playerKey, Long id);
}
