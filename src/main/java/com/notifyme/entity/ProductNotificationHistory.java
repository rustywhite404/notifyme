package com.notifyme.entity;

import jakarta.persistence.*;
import lombok.*;

//상품별 재입고 알림 히스토리
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductNotificationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int restockRound; //재입고 회차

    @Enumerated(EnumType.STRING)
    private NotifyStatus notifyStatus; //알림 발송 상태

    private Long lastSentUserId; //마지막 발송 유저 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    //알림 발송 상태 ENUM
    @AllArgsConstructor
    @Getter
    public enum NotifyStatus{
        IN_PROGRESS("발송중"),
        CANCELED_BY_SOLD_OUT("품절에 의한 발송 중단"),
        CANCELED_BY_ERROR("예외에 의한 발송 중단"),
        COMPLETED("완료");

        private final String message;

    }


}
