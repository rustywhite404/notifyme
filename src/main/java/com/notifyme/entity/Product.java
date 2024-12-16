package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int restockRound; //재입고 회차

    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus; //재고 상태

    @Version
    private Long version; //낙관적 락을 위한 버전 필드

    public enum StockStatus{
        IN_STOCK, OUT_OF_STOCK;
    }
}
