package com.notifyme.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

//상품+유저별 알림 히스토리
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductUserNotificationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; //상품 아이디(연관관계)

    private Long userId; //유저아이디
    private int restockRound; //재입고 회차
    private LocalDateTime sendAt; //발송 날짜
}
