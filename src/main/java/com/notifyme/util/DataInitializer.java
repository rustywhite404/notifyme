package com.notifyme.util;

import com.notifyme.entity.Product;
import com.notifyme.entity.ProductUserNotification;
import com.notifyme.repository.ProductRepository;
import com.notifyme.repository.ProductUserNotificationRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductUserNotificationRepository productUserNotificationRepository;

    @Override
    public void run(String... args) throws Exception {
        //이미 데이터가 존재하면 삽입하지 않음
        if (productRepository.count() == 0 || productUserNotificationRepository.count() == 0) {
            for (int i = 0; i < 5; i++) {
                Product product = productRepository.save(Product.builder()
                        .restockRound(0)
                        .stockStatus(Product.StockStatus.OUT_OF_STOCK)
                        .build());
                for (long j = 1; j <= 10; j++) {
                    productUserNotificationRepository.save(ProductUserNotification.builder()
                            .product(product)
                            .isActive(true)
                            .userId(j)
                            .build());
                }
            }
        }
    }
}
