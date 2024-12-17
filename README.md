# 재입고 알림 시스템 

## 📢 프로젝트 소개

상품이 재입고 되었을 때, 재입고 알림을 설정한 유저들에게 재입고 알림을 보내줍니다.  

![Java 17](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white) ![Spring 3.x](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![MySQL 8.0](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white) ![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)

<details>
    <summary>ERD 보기</summary>

![ERD](https://i.imgur.com/lTXFtlQ.png)

</details>

## 📢 프로젝트 실행 방법  

<details>
    <summary>프로젝트 환경 설정 및 실행 가이드</summary>

유의사항 : 사전에 **Docker**와 **Docker Compose**가 설치되어 있어야 합니다.

1. 내려받은 프로젝트의 루트 경로에 `.env` 파일을 생성하여 DB 접속정보 등의 민감한 정보를 설정해주세요.설정해야 하는 항목은 아래와 같습니다.

    ```
     REDIS_PORT=6379                # Redis가 사용할 포트
     DB_USERNAME={ROOT USERNAME}    # MySQL 데이터베이스 사용자 이름
     DB_PASSWORD={ROOT PASSWORD}    # MySQL 데이터베이스 사용자 비밀번호
     MYSQL_ROOT_PASSWORD={ROOT PASSWORD} # MySQL 루트 계정 비밀번호
    ```

`.env` 파일은 개발 환경에서 사용되며, 운영 환경에서는 별도의 `prod.env` 파일을 사용할 수 있습니다.

2. **Docker Compose로 MySQL, SpringBoot, Redis 컨테이너 실행**

   프로젝트의 docker-compose.yml 파일이 위치한 경로에서 아래 명령어를 입력해주세요.

    ```
    docker-compose up --build -d
    ```

3. DB 스키마 notifyme는 컨테이너가 시작될 때 자동으로 생성됩니다.
4. 애플리케이션은 [http://localhost:8081](http://localhost:8081/)에서 실행됩니다.


</details>

## 📢 비즈니스 로직

![비즈니스로직](https://i.imgur.com/YkHB1Pj.jpeg)

---

## 📢 주요 구현 기능

- 재입고 알림을 전송하기 전, 상품의 재입고 회차를 1 증가
- 상품이 재입고 되었을 때, 재입고 알림을 설정한 유저들에게 알림 메시지를 전달
  - ProductUserNotification 테이블에 존재하는 유저는 모두 재입고 알림을 설정했다고 가정
- 재입고 알림은 재입고 알림을 설정한 유저 **순서대로** 메시지를 전송
- 회차별 재입고 알림을 받은 유저 목록을 저장
- 재입고 알림 전송의 상태를 DB 에 저장

### 재입고 알림 전송 중 상품 품절 시 알림 중단

알람을 보낼 때 마다 DB에서 재고 상태를 확인하면 DB커넥션이 너무 많아질 것으로 판단, Redis에 상품 별 stockStatus를 저장해두고 조회하도록 함.

  ```java
  if (!isStockAvailable(productId)) {
      // 재고 부족으로 발송 중단
      updateNotificationHistory(productId, newRestockRound, NotifyStatus.CANCELED_BY_SOLD_OUT, lastSuccessfulUserId);
      log.warn("발송 중단 - 상품 ID: {}는 OUT_OF_STOCK 상태입니다.", productId);
      allNotificationsSuccessful = false; // 성공 여부를 false로 설정
      break;
  }
  
  // 알림 발송
  sendNotification(user, newRestockRound);
  ```

  ```java
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
  ```

### 알림 메시지는 1초에 최대 500개의 요청

- 서드 파티 연동을 하진 않고, ProductNotificationHistory 테이블에 데이터를 저장만 하는 요구사항

**Google Guava**의 **RateLimiter**기능을 이용하여 1초에 500개의 메시지만 일정 시간을 두고 전송하도록 처리

```java
private final RateLimiter rateLimiter = RateLimiter.create(500);
(...) 
for (ProductUserNotification user : notifiedUsers) {
            // 레이트 리미터로 초당 500명씩 처리 제한
            rateLimiter.acquire();
	          (...) 
```


## 📢 추가 기술적 고민

- **불필요한 DB 접근을 줄이고 처리 속도 개선하기**

  1초에 500건의 알림이 성공적으로 전송되어야 한다. 우선 현재 코드 상태에서 속도를 측정해보았다.

    ```java
    @Transactional
        public void restockAndNotify(Long productId) {
            (...)
                    // 알림 발송
                    sendNotification(user, newRestockRound);
    
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
    			(...) 
        }
    ```
  Code Execution Time: 937 ms  
  현재 상태에서도 1초에 500명에게 메시지 발송은 가능하지만 조금만 더 초당 발송 요구량이 증가해도 아슬아슬할 것 같다. 그리고 한 사람에게 알림을 보낼 때 마다 updateUserNotificationHistory(user, newRestockRound);를 호출하고 있어 DB에 너무 잦은 접근이 필요하다. 한 번에 모아서 배치 처리하고 이 작업을 비동기로 실행시켜 메인 로직의 성능을 개선시키기로 했다.

    ```java
    @Transactional
        public void restockAndNotify(Long productId) {
                    (...) 
                    // 알림 발송
                    sendNotification(user, newRestockRound);
    
                    // 발송 성공 : ProductUserNotificationHistory 생성 후 배치 리스트에 추가
                    batchHistories.add(ProductUserNotificationHistory.builder()
                            .product(user.getProduct())
                            .userId(user.getUserId())
                            .restockRound(newRestockRound)
                            .sendAt(LocalDateTime.now())
                            .build());
    
                } catch (Exception e) {
    							(...)
                }
            }
            //4. 배치로 ProductUserNotificationHistory 저장
            if(!batchHistories.isEmpty()){
                saveUserNotificationHistoriesInBatch(batchHistories);
            }
            (...) 
        }
    
        @Async
        private void saveUserNotificationHistoriesInBatch(List<ProductUserNotificationHistory> batchHistories) {
            try {
                log.info("배치 작업 비동기 시작: {}건의 유저 알림 히스토리 저장 중...", batchHistories.size());
                productUserNotificationHistoryRepository.saveAll(batchHistories);
                log.info("배치 작업 비동기 완료: {}건의 유저 알림 히스토리 저장 성공", batchHistories.size());
            } catch (Exception e) {
                log.error("유저 알림 히스토리 배치 저장 중 예외 발생: {}", e.getMessage(), e);
            }
        }
    ```

  Code Execution Time: 695 ms
    
  ---
  **개선 결과 :** Code Execution Time 937 ms → 695 ms 으로 수행시간 단축, DB 커넥션 500 → 1로 감소