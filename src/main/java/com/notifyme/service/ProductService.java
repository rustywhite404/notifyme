package com.notifyme.service;


import com.notifyme.entity.Product;
import com.notifyme.exception.NotifymeErrorCode;
import com.notifyme.exception.NotifymeException;
import com.notifyme.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Integer> redisTemplate;

    @Transactional
    public int increaseRestockRound(Long productId){
        // 상품 회차 증가
        Product product = productRepository.findById(productId).orElseThrow(()->new NotifymeException(NotifymeErrorCode.NO_PRODUCT));
        product.setRestockRound(product.getRestockRound()+1);

        // 레디스 캐시에 저장 (Key: "product:restockRound:{productId}")
        redisTemplate.opsForValue().set("product:restockRound:"+productId, product.getRestockRound());

        return product.getRestockRound(); //증가시킨 회차 반환
    }


}
