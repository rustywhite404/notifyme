package com.notifyme.repository;

import com.notifyme.entity.ProductUserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUserNotificationRepository extends JpaRepository<ProductUserNotification, Long> {
}
