## 프로젝트 설정 및 실행 방법
유의사항 : 사전에 Docker와 Docker Compose가 설치되어 있어야 합니다.  

1. 프로젝트를 내려받아 주세요. 
   ```bash
   git clone https://github.com/rustywhite404/notifyme.git
   cd notifyme  
   
2. 루트 경로에 `.env` 파일을 생성하여 DB 접속정보 등의 민감한 정보를 설정해주세요.  
설정해야 하는 항목은 아래와 같습니다.  
   ```env
    REDIS_PORT=6379                # Redis가 사용할 포트
    DB_USERNAME={ROOT USERNAME}    # MySQL 데이터베이스 사용자 이름
    DB_PASSWORD={ROOT PASSWORD}    # MySQL 데이터베이스 사용자 비밀번호
    MYSQL_ROOT_PASSWORD={ROOT PASSWORD} # MySQL 루트 계정 비밀번호 

`.env` 파일은 개발 환경에서 사용되며, 운영 환경에서는 별도의 `prod.env` 파일을 사용할 수 있습니다.

3. **Docker Compose로 MySQL, SpringBoot, Redis 컨테이너 실행**  
프로젝트의 docker-compose.yml 파일이 위치한 경로에서 아래 명령어를 입력해주세요. 
   ```bash
   docker-compose up --build -d

4. DB 스키마 notifyme는 컨테이너가 시작될 때 자동으로 생성됩니다.   
 
5. 애플리케이션은 http://localhost:8080 에서 실행됩니다. 

---  

### 알림 발송 처리 전, 회차 데이터 정합성 처리

- **문제 상황** :  
주어진 조건에 따르면 `/products/{productId}/notifications/re-stock` 가 호출되었을 때 상품의 재입고 회차를 증가시킨 후, 알림 발송을 요청한 사용자들에게 메시지를 발송(ProductNotificationHistory에 저장)해야 합니다. 이 때 ProductNotificationHistory에 갱신된 재입고 회차가 함께 저장되어야 하므로 트랜잭션이 종료되지 않은 상태에서도 갱신 된 재입고 회차가 반영되어야 한다고 판단했습니다.
 
- **해결 전략** :  
레디스에 최신 회차를 저장하고 즉시 조회하여 트랜잭션 내에서 갱신된 데이터를 바로 사용할 수 있도록 처리했습니다. 레디스를 사용할 수 없는 상황이라면 `Propagation.REQUIRES_NEW` 를 사용해서 처리했을 것 같습니다.

   ```java  
   @Transactional
       public int increaseRestockRound(Long productId){
           // 상품 회차 증가
           Product product = productRepository.findById(productId).orElseThrow(()->new NotifymeException(NotifymeErrorCode.NO_PRODUCT));
           product.setRestockRound(product.getRestockRound()+1);
   
           // 레디스 캐시에 저장 (Key: "product:restockRound:{productId}")
           redisTemplate.opsForValue().set("product:restockRound:"+productId, product.getRestockRound());
           return product.getRestockRound(); //증가시킨 회차 반환
       }
 

