package com.notifyme.service;


import com.notifyme.entity.Product;
import com.notifyme.exception.NotifymeErrorCode;
import com.notifyme.exception.NotifymeException;
import com.notifyme.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public int increaseRestockRound(Product product){
        int retryCount = 0;
        while (retryCount < 3) {
            try {
                // 상품 회차  +1, 저장 시 낙관적 락 적용
                product.setRestockRound(product.getRestockRound() + 1);
                productRepository.save(product);
                return product.getRestockRound(); //증가시킨 회차 반환
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                log.warn("재입고 회차 증가 충돌 발생 - 재시도: {}/3, 상품 ID: {}", retryCount, product.getId());
            }
        }
        throw new NotifymeException(NotifymeErrorCode.OPTIMISTIC_LOCK_FAIL_INCREASE_ROUND);
    }

    @Transactional
    public void updateStockStatus(Product product, Product.StockStatus newStatus) {
        if (product.getStockStatus() != newStatus) {
            product.setStockStatus(newStatus);
            productRepository.save(product);
        }
    }

    @Transactional
    public int increaseRestockRoundAndUpdateStockStatus(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotifymeException(NotifymeErrorCode.NO_PRODUCT));

        int newRestockRound = increaseRestockRound(product); // 재입고 회차 증가
        updateStockStatus(product, Product.StockStatus.IN_STOCK); // 재고 상태 변경

        //레디스 데이터 갱신
        redisTemplate.opsForValue().set("product:stockStatus:" + productId, product.getStockStatus().name());
        log.info("Redis 갱신 완료 - productId: {}, restockRound: {}, stockStatus: {}",
                productId, product.getRestockRound(), product.getStockStatus());

        return newRestockRound;
    }

    public Product findProductById(Long productId){
        Product product = productRepository.findById(productId).orElseThrow(()->new NotifymeException(NotifymeErrorCode.NO_PRODUCT));
        return product;
    }

    public Product.StockStatus getStockStatus(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotifymeException(NotifymeErrorCode.NO_PRODUCT));

        //상품의 재고 상태 반환
        return product.getStockStatus();
    }


}
