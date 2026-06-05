package com.anipals.backend.trade.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trade_offer_items")
@Getter
@Setter
@NoArgsConstructor
public class TradeOfferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_offer_id", nullable = false)
    private TradeOffer tradeOffer;

    @Column(nullable = false)
    private Long sourceInventoryItemId;

    @Column(nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String rarity;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String color;
}
