package com.notifyme.controller;

import com.notifyme.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final NotificationService notificationService;
    @PostMapping(value = "/{productId}/notifications/re-stock")
    public void sendNotifyMessage(@PathVariable("productId") Long productId) {
        log.info("-------------productId: {}", productId);
        notificationService.restockAndNotify(productId);
    }

}
