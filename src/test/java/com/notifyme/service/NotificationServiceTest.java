package com.notifyme.service;

import com.notifyme.entity.Product;
import com.notifyme.entity.ProductUserNotification;
import com.notifyme.repository.ProductRepository;
import com.notifyme.repository.ProductUserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;


@SpringBootTest
public class NotificationServiceTest {
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUserNotificationRepository productUserNotificationRepository;
    private Long productId;

    @BeforeEach
    public void setup() {
        // 테스트용 데이터 준비 (Product와 User 알림 데이터 생성)
        productId = prepareTestData();
    }

    @Test
    public void testCodePerformance() throws InterruptedException {
        // 코드 실행 시간 측정
        long startTime = System.nanoTime();
        notificationService.restockAndNotify(productId);
        long endTime = System.nanoTime();

        System.out.println("Code Execution Time: " + (endTime - startTime) / 1_000_000 + " ms");
    }


    private Long prepareTestData() {
        // 1. 상품 생성
        Product product = Product.builder()
                .restockRound(0)
                .stockStatus(Product.StockStatus.IN_STOCK)
                .build();
        product = productRepository.save(product);

        //2. 500명의 알림 신청 유저 생성
        List<ProductUserNotification> notifications = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            ProductUserNotification notification = ProductUserNotification.builder()
                    .product(product)
                    .userId((long) i)
                    .isActive(true)
                    .build();
            notifications.add(notification);
        }
        productUserNotificationRepository.saveAll(notifications);

        return product.getId();

    }


}