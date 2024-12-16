package com.notifyme.service;

import com.notifyme.entity.Product;
import com.notifyme.entity.ProductNotificationHistory;
import com.notifyme.entity.ProductNotificationHistory.NotifyStatus;
import com.notifyme.entity.ProductUserNotification;
import com.notifyme.entity.ProductUserNotificationHistory;
import com.notifyme.repository.ProductNotificationHistoryRepository;
import com.notifyme.repository.ProductUserNotificationHistoryRepository;
import com.notifyme.repository.ProductUserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final ProductService productService;
    private final ProductUserNotificationRepository productUserNotificationRepository;
    private final ProductNotificationHistoryRepository productNotificationHistoryRepository;
    private final ProductUserNotificationHistoryRepository productUserNotificationHistoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void restockAndNotify(Long productId) {
        //1. 재입고 회차 증가
        int newRestockRound = productService.increaseRestockRound(productId);

        //2. ProductNotificationHistory의 해당 상품에 재입고 회차를 갱신하고, 알림 발송 상태를 IN_PROGRESS로 갱신한다.
        updateNotificationHistory(productId, newRestockRound, NotifyStatus.IN_PROGRESS, null);

        //3. 해당 상품의 알림을 신청한 유저 리스트 조회
        List<ProductUserNotification> notifiedUsers = productUserNotificationRepository.findByProductId(productId);

        Long lastSuccessfulUserId = null; // 마지막으로 성공한 유저 ID 추적
        boolean allNotificationsSuccessful = true; // 모든 알림 성공 여부 확인
        //4. 알림을 신청한 유저들에게 알림을 발송하고 상품ID, 유저ID, 재입고회차, 발송 날짜를 ProductUserNotificationHistory에 저장한다.
        for (ProductUserNotification user : notifiedUsers) {
            try {
                // 상품의 재고 상태 확인
                if (!isStockAvailable(productId)) {
                    // 재고 부족으로 발송 중단
                    updateNotificationHistory(productId, newRestockRound, NotifyStatus.CANCELED_BY_SOLD_OUT, lastSuccessfulUserId);
                    log.warn("발송 중단 - 상품 ID: {}는 OUT_OF_STOCK 상태입니다.", productId);
                    allNotificationsSuccessful = false; // 성공 여부를 false로 설정
                    break;
                }

                // 알림 발송
                sendNotification(user, newRestockRound);

                // 발송 성공 : ProductNotificationHistory 업데이트
                lastSuccessfulUserId = user.getUserId(); // 성공한 유저 ID 추적

                // 발송 성공 : ProductUserNotificationHistory 업데이트
                updateUserNotificationHistory(user, newRestockRound);

            } catch (Exception e) {
                // 예외 발생 시 상태 업데이트
                updateNotificationHistory(productId, newRestockRound, NotifyStatus.CANCELED_BY_ERROR, lastSuccessfulUserId);
                log.error("알림 발송 중 예외 발생 - 상품 ID: {}, 에러: {}", productId, e.getMessage(), e);
                allNotificationsSuccessful = false;
                break;
            }
        }

        // 5. 모든 알림 발송이 성공적으로 완료된 경우에만 COMPLETED 상태로 업데이트
        if (allNotificationsSuccessful) {
            updateNotificationHistory(productId, newRestockRound, NotifyStatus.COMPLETED, lastSuccessfulUserId);
            log.info("모든 알림 발송 성공 - 상품 ID: {}, 재입고 회차: {}", productId, newRestockRound);
        }

    }

    private void updateUserNotificationHistory(ProductUserNotification user, int restockRound) {

        Product product = productService.findProductById(user.getProduct().getId());

        // 발송 히스토리 저장
        ProductUserNotificationHistory history = ProductUserNotificationHistory.builder()
                .product(product)
                .userId(user.getUserId())
                .restockRound(restockRound)
                .sendAt(LocalDateTime.now())
                .build();
        productUserNotificationHistoryRepository.save(history);
    }

    private boolean isStockAvailable(Long productId) {
        // Redis를 통해 상품 재고 상태 확인
        String stockKey = "product:stockStatus:" + productId;
        String stockStatus = redisTemplate.opsForValue().get(stockKey);

        if (stockStatus == null) {
            stockStatus = productService.getStockStatus(productId).name();
            redisTemplate.opsForValue().set(stockKey, stockStatus);
        }
        // IN_STOCK 여부 반환
        return "IN_STOCK".equals(stockStatus);
    }

    private void updateNotificationHistory(Long productId, int restockRound, NotifyStatus status, Long lastSentUserId) {

        // Product 객체 조회
        Product product = productService.findProductById(productId); // ProductService를 통해 조회

        // 히스토리를 조회하거나 없으면 생성
        ProductNotificationHistory history = productNotificationHistoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    log.info("기존 히스토리가 존재하지 않아 새로 생성합니다 - 상품 ID: {}, 재입고 회차: {}", productId, restockRound);
                    return ProductNotificationHistory.builder()
                            .product(product) // Product 객체 주입
                            .restockRound(restockRound)
                            .notifyStatus(NotifyStatus.IN_PROGRESS) // 기본 상태
                            .lastSentUserId(null) // 초기값은 null
                            .build();
                });

        // 기존 lastSentUserId를 유지하거나 새 값으로 업데이트
        if (lastSentUserId != null) {
            history.setLastSentUserId(lastSentUserId);
        }

        // 히스토리 상태 업데이트
        history.setNotifyStatus(status);
        history.setRestockRound(restockRound);

        // 저장
        productNotificationHistoryRepository.save(history);

        // 트랜잭션 동기화: Redis 상태 반영
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                String redisKey = "product:notificationStatus:" + productId + ":" + restockRound;
                redisTemplate.opsForValue().set(redisKey, status.name());
                log.info("Redis에 알림 상태 저장 - Key: {}, 상태: {}", redisKey, status);
            }
        });
    }

    private void sendNotification(ProductUserNotification user, int newRestockRound) {
        // 알림 발송 로직. 비즈니스 요구 조건에서 따로 요구사항이 없어 로그만 기록
        log.info("알림 발송 - 상품 ID: {}, 유저 ID: {}, 재입고 회차: {}",
                user.getProduct().getId(),
                user.getUserId(),
                newRestockRound);
    }


}
